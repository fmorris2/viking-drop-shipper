package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class FulfillmentManager {
	
	public static final int SAFE_STOCK_THRESHOLD = 100;
	
	private static final long ONE_DAY_MS = (60000 * 60) * 24;
	private static final double SAMS_ORDER_BATCH_WINDOW = 2.0D; //2.0 days is the longest we'll wait before fulfilling an order
	private static final int SAMS_SAFE_NUM_ORDERS_THRESHOLD = 19;
	
	//pair = month,day
	private static final List<Pair<Integer, Integer>> USPS_HOLIDAYS = Arrays.asList(
		new Pair<>(1,1), //New Years Day
		new Pair<>(1,20), //Martin Luther King Jr. Day
		new Pair<>(2,17), //Washington's Birthday
		new Pair<>(5,25), // Memorial Day
		new Pair<>(7,4), //Independence Day
		new Pair<>(10,12), //Columbus Day
		new Pair<>(11,11), //Veteran's Day
		new Pair<>(11,26), //Thanksgiving Day
		new Pair<>(12,25) //Christmas Day
	);

	private static final Set<Integer> canExecuteOrdersPlatforms = new HashSet<>();
	private static final Set<Integer> canUpdateInventoryPlatforms = new HashSet<>();

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

	public static final boolean canExecuteOrders(final int fulfillmentPlatformId) {
		return canExecuteOrdersPlatforms.contains(fulfillmentPlatformId);
	}
	
	public static final boolean canUpdateInventory(final int fulfillmentPlatformId) {
		return canUpdateInventoryPlatforms.contains(fulfillmentPlatformId);
	}
	
	private static boolean isBatchingOrders() {
		return true;
	}

	public static void disableOrderExecution(final int fulfillmentPlatformId) {
		canExecuteOrdersPlatforms.add(fulfillmentPlatformId);
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			st.execute("UPDATE fulfillment_platform SET can_execute_orders=0 WHERE id=" + fulfillmentPlatformId);
		} catch(final Exception e) {
			DBLogging.critical(FulfillmentManager.class, "failed to disable order execution for fulfillment platform " + fulfillmentPlatformId, e);
		}
	}
	
	public boolean shouldFulfill(final CustomerOrder order, final FulfillmentListing listing) {
		if(listing.fulfillment_platform_id == FulfillmentPlatforms.SAMS_CLUB.getId()) {
			return shouldFulfillSamsClubOrder(order, listing);
		}
		
		return true;
	}
	
	private boolean shouldFulfillSamsClubOrder(final CustomerOrder order, final FulfillmentListing listing) {
		//TODO MODIFY LOGIC TO ACCOUNT FOR MULTIPLE SAMS CLUB ACCOUNTS?
		final FulfillmentAccount acc = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.SAMS_CLUB);
		if(acc == null) {
			System.out.println("There are currently no enabled sams club accounts.");
			return false;
		}
		
		final int numOrders = FulfillmentAccountManager.get().getNumProcessedOrdersForAccount(acc.id);
		final double businessDaysSinceOrder = getBusinessDaysSinceOrder(order.date_parsed);
		final int stock = SamsClubFulfillmentStockChecker.get().parseItemStock(listing);
		System.out.println("Business Days Since Order: " + businessDaysSinceOrder);
		
		final boolean failsSafeOrderThreshold = numOrders > SAMS_SAFE_NUM_ORDERS_THRESHOLD;
		final boolean failsTimeWindowThreshold = businessDaysSinceOrder > SAMS_ORDER_BATCH_WINDOW;
		final boolean failsLowStockThreshold = stock < SAFE_STOCK_THRESHOLD;
		if(!isBatchingOrders() || failsSafeOrderThreshold || failsTimeWindowThreshold || failsLowStockThreshold) {
			return true;
		}
		
		System.out.println("Skipping fulfillment of Sam's Club order for customer order " + order.id + " due to order batching.");
		return false;
	}
	
	private static double getBusinessDaysSinceOrder(final long orderDateParsed) {
		final LocalDateTime current = LocalDateTime.now();
		
		LocalDateTime temp = LocalDateTime.ofInstant(Instant.ofEpochMilli(orderDateParsed), ZoneId.systemDefault());	
		double businessDaysSinceOrder = 0;
		
		while(temp.isBefore(current)) {
			final LocalDateTime tempPlusOneDay = temp.plusDays(1);
			if(isBusinessDay(temp)) {
				if(isBusinessDay(tempPlusOneDay)) {
					if(tempPlusOneDay.isAfter(current)) {
						businessDaysSinceOrder += getDaysBetween(temp, current);
						temp = current;
					} else {
						temp = tempPlusOneDay;
						businessDaysSinceOrder += 1;
					}
				} else {
					businessDaysSinceOrder += getDaysBetween(temp, tempPlusOneDay.truncatedTo(ChronoUnit.DAYS));
					temp = tempPlusOneDay.truncatedTo(ChronoUnit.DAYS);
				}
			} else {
				temp = temp.plusDays(1).truncatedTo(ChronoUnit.DAYS);
			}
		}
		
		return businessDaysSinceOrder;
	}
	
	private static double getDaysBetween(final LocalDateTime min, final LocalDateTime max) {
		final ZoneOffset offset = OffsetDateTime.now().getOffset();
		return (max.toInstant(offset).toEpochMilli() - min.toInstant(offset).toEpochMilli()) / (double)ONE_DAY_MS;
	}
	
	private static boolean isBusinessDay(final LocalDateTime date) {
		final DayOfWeek day = date.getDayOfWeek();
		return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
				&& !isHoliday(date);
	}
	
	private static boolean isHoliday(final LocalDateTime date) {
		for(final Pair<Integer,Integer> holiday : USPS_HOLIDAYS) {
			if(date.getMonthValue() == holiday.left && date.getDayOfMonth() == holiday.right) {
				return true;
			}
		}
		
		return false;
	}
	
	public static void main(final String[] args) {
		System.out.println(getBusinessDaysSinceOrder(1577557816004L));
		System.out.println(getBusinessDaysSinceOrder(1577552732653L));
		System.out.println(getBusinessDaysSinceOrder(1577530625782L));
		System.out.println(getBusinessDaysSinceOrder(1577502409032L));
		System.out.println(getBusinessDaysSinceOrder(1577484718366L));
		System.out.println(getBusinessDaysSinceOrder(1577474604448L));
		System.out.println(getBusinessDaysSinceOrder(1577293916170L));
	}

	public void load() {
		canExecuteOrdersPlatforms.clear();
		canUpdateInventoryPlatforms.clear();
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
		final FulfillmentAccount account = FulfillmentAccountManager.get().peekEnabledAccount(applicablePlatform);
		if(account == null) {
			throw new OrderExecutionException("There are currently no enabled accounts for fulfillment platform " + applicablePlatform);
		}
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
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing WHERE fulfillment_platform_id=" + platformId +
					" AND item_id="+itemId)) {

			if(results.next()) {
				final FulfillmentListing listing = new FulfillmentListing.Builder()
						.id(results.getInt("id"))
						.fulfillment_platform_id(platformId)
						.item_id(itemId)
						.upc(results.getString("upc"))
						.ean(results.getString("ean"))
						.product_id(results.getString("product_id"))
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
	
	public List<FulfillmentListing> getListingsForFulfillmentPlatform(final FulfillmentPlatforms platform) {
		final List<FulfillmentListing> listings = new ArrayList<>();
		try(
		   final Statement st = VSDSDBManager.get().createStatement();
		   final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing WHERE fulfillment_platform_id=" + platform.getId())) {
			
			while(results.next()) {
				final FulfillmentListing listing = new FulfillmentListing.Builder()
						.id(results.getInt("id"))
						.fulfillment_platform_id(platform.getId())
						.item_id(results.getString("item_id"))
						.upc(results.getString("upc"))
						.ean(results.getString("ean"))
						.product_id(results.getString("product_id"))
						.listing_title(results.getString("listing_title"))
						.listing_url(results.getString("listing_url"))
						.build();
				listings.add(listing);
			}
			
			
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return listings;
	}

	public List<FulfillmentListing> getListingsForOrder(final CustomerOrder order) {
		return listings.getOrDefault(order.marketplace_listing_id, new ArrayList<>());
	}

	public List<FulfillmentListing> getListingsForMarketplaceListing(final int marketplaceListingId) {
		return listings.getOrDefault(marketplaceListingId, new ArrayList<>());
	}

	private void loadValidFulfillmentListings() {
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing"
					+ " INNER JOIN fulfillment_mapping ON"
					+ " fulfillment_listing.id=fulfillment_mapping.fulfillment_listing_id")) {
			while(results.next()) {
				final int marketplace_listing_id = results.getInt("marketplace_listing_id");
				final FulfillmentListing listing = new FulfillmentListing.Builder()
					.id(results.getInt("fulfillment_listing.id"))
					.fulfillment_platform_id(results.getInt("fulfillment_platform_id"))
					.item_id(results.getString("item_id"))
					.upc(results.getString("upc"))
					.ean(results.getString("ean"))
					.product_id(results.getString("product_id"))
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
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_platform")) {
			while(results.next()) {
				final FulfillmentPlatform platform = new FulfillmentPlatform.Builder()
					.id(results.getInt("id"))
					.platform_name(results.getString("platform_name"))
					.platform_url(results.getString("platform_url"))
					.can_execute_orders(results.getBoolean("can_execute_orders"))
					.can_update_inventory(results.getBoolean("can_update_inventory"))
					.build();

				if(platform.can_execute_orders) {
					canExecuteOrdersPlatforms.add(platform.id);
				}
				
				if(platform.can_update_inventory) {
					canUpdateInventoryPlatforms.add(platform.id);
				}
				
				platforms.put(platform.id, platform);
			}
		} catch(final SQLException e) {
			DBLogging.high(FulfillmentManager.class, "failed to load fulfillment platforms: ", e);
		}
	}
}
