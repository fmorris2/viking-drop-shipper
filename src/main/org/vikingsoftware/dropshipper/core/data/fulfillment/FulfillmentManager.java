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
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class FulfillmentManager {
	
	private static final Set<Integer> frozenFulfillmentPlatforms = new HashSet<>();
	
	private static FulfillmentManager instance;
	
	//marketplace listing id ==> fulfillment listing
	private final Map<Integer, List<FulfillmentListing>> listings = new HashMap<>();
	
	//db primary key id ==> fulfillment platform
	private final Map<Integer, FulfillmentPlatform> platforms = new HashMap<>();
	
	private final Map<FulfillmentPlatforms, OrderExecutionStrategy> strategies = new HashMap<>();
	private final Map<Integer, List<FulfillmentMapping>> mappings = new HashMap<>();
	
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
			final Statement st = VDSDBManager.get().createStatement();
			st.execute("UPDATE fulfillment_platform SET frozen=1 WHERE id=" + fulfillmentPlatformId);
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public void load() {
		frozenFulfillmentPlatforms.clear();
		listings.clear();
		platforms.clear();
		mappings.clear();
		loadFulfillmentListings();
		loadFulfillmentPlatforms();
	}
	
	public boolean prepareForFulfillment() {
		load();
		for(final FulfillmentPlatforms platform : FulfillmentPlatforms.values()) {
			final OrderExecutionStrategy strategy = platform.generateStrategy();
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
		return strategy.order(order, listing);
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
	
	public List<FulfillmentListing> getListingsForOrder(final CustomerOrder order) {
		return listings.getOrDefault(order.marketplace_listing_id, new ArrayList<>());
	}
	
	public List<FulfillmentListing> getListingsForMarketplaceListing(final int marketplaceListingId) {
		return listings.getOrDefault(marketplaceListingId, new ArrayList<>());
	}
	
	public Optional<FulfillmentMapping> getFulfillmentMapping(final int marketplaceListingId, final int fulfillmentListingId) {
		final List<FulfillmentMapping> mappingz = mappings.getOrDefault(marketplaceListingId, new ArrayList<>());
		for(final FulfillmentMapping mapping : mappingz) {
			if(mapping.fulfillment_listing_id == fulfillmentListingId) {
				return Optional.of(mapping);
			}
		}
		
		return Optional.empty();
	}
	
	public List<FulfillmentMapping> getFulfillmentMappings(final int marketplaceListingId) {
		return mappings.getOrDefault(marketplaceListingId, new ArrayList<>());
	}
	
	private void loadFulfillmentListings() {
		final Statement st = VDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing"
					+ " INNER JOIN fulfillment_mapping ON"
					+ " fulfillment_listing.id=fulfillment_mapping.fulfillment_listing_id");
			while(results.next()) {
				final int marketplace_listing_id = results.getInt("marketplace_listing_id");
				final FulfillmentListing listing = new FulfillmentListing.Builder()
					.id(results.getInt("fulfillment_listing.id"))
					.fulfillment_platform_id(results.getInt("fulfillment_platform_id"))
					.listing_id(results.getString("listing_id"))
					.listing_url(results.getString("listing_url"))
					.listing_max_price(results.getDouble("listing_max_price"))
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
		final Statement st = VDSDBManager.get().createStatement();
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
			e.printStackTrace();
		}
	}
}
