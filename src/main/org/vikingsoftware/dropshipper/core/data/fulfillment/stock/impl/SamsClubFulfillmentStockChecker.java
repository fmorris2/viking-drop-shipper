package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentListingStockEntry;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubProductAPI;

public class SamsClubFulfillmentStockChecker implements FulfillmentStockChecker {

	public static final double SAMS_CLUB_SHIPPING_RATE = 5.00; //$5
	
	private static final String AVG_SHIPPING_COST_QUERY = "select fulfillment_listing_id, "
			+ "SUM(buy_shipping)/COUNT(*) as average_shipping_cost from processed_order "
			+ "where buy_shipping > 0 group by fulfillment_listing_id";
	
	private static final String ESTIMATED_SHIPPING_COST_QUERY = "select id,estimated_shipping_cost FROM fulfillment_listing"
			+ " WHERE estimated_shipping_cost IS NOT NULL";
	
	private static final long RECOMPUTE_AVG_SHIPPING_COSTS_CYCLE_TIME = 60_000 * 30;
	
	private static final Map<String, Double> averageShippingCosts = new ConcurrentHashMap<>();
	private static long lastAvgShippingCostRecompute;
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	private final SamsClubProductAPI api = new SamsClubProductAPI();
	
	@Override
	public Optional<FulfillmentListingStockEntry> getStock(FulfillmentAccount account, FulfillmentListing fulfillmentListing) {
		if(api.parse(fulfillmentListing.product_id)) {		
			if(!fulfillmentListing.item_id.equalsIgnoreCase(api.getItemNumber().orElse(null))) {
				System.out.println("Could not parse metadata as the item IDs don't match!");
				final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
						.successfullyParsedDetails(false)
						.build();
						
				return Optional.of(entry);
			}
			
			int stock = api.getAvailableToSellQuantity().orElse(0);
			if(stock < FulfillmentManager.SAFE_STOCK_THRESHOLD) {
				stock = 0;
			}
			final double shippingCost = generateShippingCostForItem(fulfillmentListing);
			double price = api.getListPrice().orElse(-1D);
			final int minPurchaseQty = api.getMinPurchaseQty();
			final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
					.successfullyParsedDetails(true)
					.minPurchaseQty(minPurchaseQty)
					.stock(stock)
					.price((price * minPurchaseQty) + shippingCost)
					.build();
			return Optional.of(entry);
		}
		
		return Optional.empty();
	}
	
	private double generateShippingCostForItem(final FulfillmentListing listing) {
		lock.writeLock().lock();
		try {
			if(System.currentTimeMillis() - lastAvgShippingCostRecompute > RECOMPUTE_AVG_SHIPPING_COSTS_CYCLE_TIME) {
				recomputeAvgShippingCosts();
				lastAvgShippingCostRecompute = System.currentTimeMillis();
			}
		} finally {
			lock.writeLock().unlock();
		}
		
		return averageShippingCosts.getOrDefault(listing.id, SAMS_CLUB_SHIPPING_RATE);
	}
	
	private void recomputeAvgShippingCosts() {
		averageShippingCosts.clear();
		try(final Statement st1 = VSDSDBManager.get().createStatement();
			final Statement st2 = VSDSDBManager.get().createStatement();
			final ResultSet res1 = st1.executeQuery(AVG_SHIPPING_COST_QUERY);
			final ResultSet res2 = st2.executeQuery(ESTIMATED_SHIPPING_COST_QUERY)) {
			
			while(res1.next()) {
				averageShippingCosts.put(res1.getString("fulfillment_listing_id"), res1.getDouble("average_shipping_cost"));
			}
			
			while(res2.next()) {
				final String listingId = res2.getString("id");
				if(!averageShippingCosts.containsKey(listingId)) {
					averageShippingCosts.put(listingId, res2.getDouble("estimated_shipping_cost"));
				}
			}
			
			System.out.println("Computed " + averageShippingCosts.size() + " average shipping costs");
		} catch(final Exception e)  {
			e.printStackTrace();
		}
	}
}
