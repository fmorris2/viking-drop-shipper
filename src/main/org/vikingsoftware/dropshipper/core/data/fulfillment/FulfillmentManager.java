package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class FulfillmentManager {

	private static final Set<Integer> frozenFulfillmentPlatforms = new HashSet<>();

	private static FulfillmentManager instance;

	//marketplace listing id ==> fulfillment listing
	private final Map<Integer, List<FulfillmentListing>> listings = new HashMap<>();

	//db primary key id ==> fulfillment platform
	private final Map<Integer, FulfillmentPlatform> platforms = new HashMap<>();

	private final Map<FulfillmentPlatforms, OrderExecutionStrategy> strategies = new HashMap<>();

	private FulfillmentManager() {

	}

	public static FulfillmentManager get() {
		if(instance == null) {
			instance = new FulfillmentManager();
		}

		return instance;
	}

	public static final boolean isFrozen(final int fulfillmentPlatformId) {
		return frozenFulfillmentPlatforms.contains(fulfillmentPlatformId);
	}

	public static void freeze(final int fulfillmentPlatformId) {
		frozenFulfillmentPlatforms.add(fulfillmentPlatformId);
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("UPDATE fulfillment_platform SET frozen=1 WHERE id=" + fulfillmentPlatformId);
		} catch(final Exception e) {
			DBLogging.critical(FulfillmentManager.class, "failed to freeze fulfillment platform " + fulfillmentPlatformId, e);
		}
	}
	
	public boolean shouldFulfill(final CustomerOrder order, final FulfillmentListing listing) {		
		return true;
	}

	public void flagFulfillmentListingForExamination(final FulfillmentListing listing, final String newTitle) {
		System.out.println("Flagging listing " + listing + " for examination");
		for(final List<FulfillmentListing> listingsForOrder : listings.values()) {
			listingsForOrder.remove(listing);
		}

		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("INSERT INTO fulfillment_listings_to_examine(fulfillment_listing_id, current_listing_title)"
					+ " VALUES("+listing.id+",'"+newTitle+"')");
			System.out.println("Successfully flagged listing " + listing + " for examination");
		} catch(final Exception e) {
			DBLogging.high(FulfillmentManager.class, "failed to flag listing " + listing + " for examination: " , e);
		}
	}

	public void load() {
		frozenFulfillmentPlatforms.clear();
		listings.clear();
		platforms.clear();
		loadValidFulfillmentListings();
		loadFulfillmentPlatforms();
	}

	public boolean prepareForFulfillment() {
		load();
		for(final FulfillmentPlatforms platform : FulfillmentPlatforms.values()) {
			System.out.println("Generating strategy for " + platform);
			final OrderExecutionStrategy strategy = platform.generateStrategy();
			if(strategy == null) {
				continue;
			}
			if(!strategy.prepareForExecution()) {
				return false;
			}
			strategies.put(platform, strategy);
		}

		return true;
	}

	public ProcessedOrder fulfill(final CustomerOrder order, final FulfillmentListing listing) {
		final FulfillmentPlatforms applicablePlatform = FulfillmentPlatforms.getById(listing.fulfillment_platform_id);
		System.out.println("Applicable fulfillment platform for order " + order.id + ": " + applicablePlatform);
		final OrderExecutionStrategy strategy = strategies.get(applicablePlatform);
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAndRotateEnabledAccount(applicablePlatform);
		return strategy.order(order, account, listing);
	}

	public void endFulfillment() {
		for(final OrderExecutionStrategy strategy : strategies.values()) {
			strategy.finishExecution();
		}

		strategies.clear();
	}

	public boolean isLoaded() {
		return !listings.isEmpty() && !platforms.isEmpty();
	}

	public Optional<FulfillmentListing> getListingForProcessedOrder(final ProcessedOrder order) {
		for(final List<FulfillmentListing> list : listings.values()) {
			for(final FulfillmentListing listing : list) {
				if(listing.id == order.fulfillment_listing_id) {
					return Optional.of(listing);
				}
			}
		}

		return Optional.empty();
	}

	public Optional<FulfillmentListing> getListingForItemId(final int platformId, final String itemId) {
		final Statement st = VSDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing WHERE fulfillment_platform_id=" + platformId +
					" AND item_id="+itemId);

			if(results.next()) {
				final FulfillmentListing listing = new FulfillmentListing.Builder()
						.id(results.getInt("id"))
						.fulfillment_platform_id(platformId)
						.item_id(itemId)
						.listing_title(results.getString("listing_title"))
						.listing_url(results.getString("listing_url"))
						.build();

				return Optional.of(listing);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return Optional.empty();
	}

	public List<FulfillmentListing> getListingsForOrder(final CustomerOrder order) {
		return listings.getOrDefault(order.marketplace_listing_id, new ArrayList<>());
	}

	public List<FulfillmentListing> getListingsForMarketplaceListing(final int marketplaceListingId) {
		return listings.getOrDefault(marketplaceListingId, new ArrayList<>());
	}

	private void loadValidFulfillmentListings() {
		final Statement st = VSDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing"
					+ " INNER JOIN fulfillment_mapping ON"
					+ " fulfillment_listing.id=fulfillment_mapping.fulfillment_listing_id"
					+ " LEFT JOIN fulfillment_listings_to_examine ON"
					+ " fulfillment_listing.id=fulfillment_listings_to_examine.fulfillment_listing_id"
					+ "	WHERE fulfillment_listings_to_examine.id IS NULL");
			while(results.next()) {
				final int marketplace_listing_id = results.getInt("marketplace_listing_id");
				final FulfillmentListing listing = new FulfillmentListing.Builder()
					.id(results.getInt("fulfillment_listing.id"))
					.fulfillment_platform_id(results.getInt("fulfillment_platform_id"))
					.item_id(results.getString("item_id"))
					.listing_title(results.getString("listing_title"))
					.listing_url(results.getString("listing_url"))
					.build();

				final List<FulfillmentListing> currentListings = listings.getOrDefault(marketplace_listing_id, new ArrayList<>());
				currentListings.add(listing);
				listings.put(marketplace_listing_id, currentListings);
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadFulfillmentPlatforms() {
		final Statement st = VSDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_platform");
			while(results.next()) {
				final FulfillmentPlatform platform = new FulfillmentPlatform.Builder()
					.id(results.getInt("id"))
					.platform_name(results.getString("platform_name"))
					.platform_url(results.getString("platform_url"))
					.frozen(results.getBoolean("frozen"))
					.build();

				if(platform.frozen) {
					frozenFulfillmentPlatforms.add(platform.id);
				}
				platforms.put(platform.id, platform);
			}
		} catch(final SQLException e) {
			DBLogging.high(FulfillmentManager.class, "failed to load fulfillment platforms: ", e);
		}
	}
}
