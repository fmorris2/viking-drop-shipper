package main.org.vikingsoftware.dropshipper.pricing.margins;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public class MarginAdjuster implements CycleParticipant {
	
	/*
	 * We only perform margin updates for a specific listing a maximum of one time
	 * every LAST_MARGIN_UPDATE_TIME_WINDOW
	 */
	private static final long LAST_MARGIN_UPDATE_TIME_WINDOW = ((60_000 * 60) * 24) * 7; //1 week
	
	/*
	 * We only consider a listing stagnant if it hasn't had a single order in the last
	 * LAST_ORDER_TIME_WINDOW
	 */
	private static final long LAST_ORDER_TIME_WINDOW = ((60_000 * 60) * 24) * 7;
	
	/*
	 * If a marketplace listing has sold this many orders in the LAST_ORDER_TIME_WINDOW,
	 * we'll consider it a productive listing
	 */
	private static final int MIN_PRODUCTIVE_ORDERS_IN_LAST_ORDER_TIME_WINDOW = 2;
	
	private static final double MARGIN_FLOOR = 8.0; //8% margin is the lowest we'll go
	private static final double MARGIN_ADJUSTMENT_STEP = 1.0; //We'll change the margin by this amount on each adjustment
	
	private static final String STAGNANT_LISTING_QUERY = "SELECT marketplace_listing.id as stagnantMarketplaceListingID, marketplace_listing.target_margin as currentMargin, marketplace_listing.listing_title as listingTitle" + 
			" FROM marketplace_listing" + 
			" INNER JOIN fulfillment_mapping ON fulfillment_mapping.marketplace_listing_id = marketplace_listing.id" + 
			" LEFT JOIN (" + 
			"(SELECT processed_order.fulfillment_listing_id, processed_order.date_processed" + 
			" FROM processed_order" + 
			" WHERE ((UNIX_TIMESTAMP() * 1000) - processed_order.date_processed > " + LAST_ORDER_TIME_WINDOW + ")" + 
			" GROUP BY processed_order.fulfillment_listing_id" + 
			" ORDER BY date_processed DESC) AS t2" + 
			") ON fulfillment_mapping.fulfillment_listing_id = t2.fulfillment_listing_id" + 
			" WHERE (UNIX_TIMESTAMP() * 1000) - marketplace_listing.last_margin_update > " + LAST_MARGIN_UPDATE_TIME_WINDOW;
	
	private static final String PRODUCTIVE_LISTING_QUERY = "SELECT * FROM (SELECT marketplace_listing.id AS productiveMarketplaceListingID, marketplace_listing.target_margin AS currentMargin," + 
			" marketplace_listing.listing_title AS listingTitle, COUNT(marketplace_listing.id) AS num_orders" + 
			" FROM marketplace_listing" + 
			" INNER JOIN customer_order ON customer_order.marketplace_listing_id=marketplace_listing.id" + 
			" WHERE (UNIX_TIMESTAMP() * 1000) - customer_order.date_parsed <= " + LAST_ORDER_TIME_WINDOW +
			" AND (UNIX_TIMESTAMP() * 1000) - marketplace_listing.last_margin_update > " + LAST_MARGIN_UPDATE_TIME_WINDOW +
			" GROUP BY marketplace_listing.id) sub_query WHERE num_orders >= " + MIN_PRODUCTIVE_ORDERS_IN_LAST_ORDER_TIME_WINDOW;
	
	private boolean debug = true;
	
	public static void main(final String[] args) {
		//System.out.println(STAGNANT_LISTING_QUERY);
		//System.out.println(PRODUCTIVE_LISTING_QUERY);
		new MarginAdjuster().cycle();
	}

	@Override
	public void cycle() {
		debug("Cycling...");
		
		final List<ListingDueForMarginAdjustment> stagnantListings = getStagnantListings();
		debug("There are currently " + stagnantListings.size() + " stagnant listings to adjust...");
		
		final List<ListingDueForMarginAdjustment> productiveListings = getProductiveListings();
		debug("There are currently " + productiveListings.size() + " productive listings to adjust...");
		
		final List<ListingDueForMarginAdjustment> listingsToAdjust = new ArrayList<>();
		listingsToAdjust.addAll(stagnantListings);
		listingsToAdjust.addAll(productiveListings);
		
		for(final ListingDueForMarginAdjustment listing : listingsToAdjust) {
			adjustListing(listing);
		}
		
		
	}
	
	/*
	 * A Listing is considered "Stagnant" if the following conditions are true:
	 *    - The last margin update for the listing is > MARGIN_UPDATE_TIME_WINDOW
	 *    - The last order for the listing is > LAST_ORDER_TIME_WINDOW   	
	 */
	private final List<ListingDueForMarginAdjustment> getStagnantListings() {
		final List<ListingDueForMarginAdjustment> stagnantListings = new ArrayList<>();
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery(STAGNANT_LISTING_QUERY)) {
			while(res.next()) {
				final ListingDueForMarginAdjustment listing = new ListingDueForMarginAdjustment(res.getInt("stagnantMarketplaceListingID"),
						res.getDouble("currentMargin"), res.getString("listingTitle"), MarginAdjustmentType.STAGNANT);
				stagnantListings.add(listing);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return stagnantListings;
	}
	
	private final List<ListingDueForMarginAdjustment> getProductiveListings() {
		final List<ListingDueForMarginAdjustment> stagnantListings = new ArrayList<>();
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery(PRODUCTIVE_LISTING_QUERY)) {
			while(res.next()) {
				final ListingDueForMarginAdjustment listing = new ListingDueForMarginAdjustment(res.getInt("productiveMarketplaceListingID"),
						res.getDouble("currentMargin"), res.getString("listingTitle"), MarginAdjustmentType.PRODUCTIVE);
				stagnantListings.add(listing);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return stagnantListings;
	}
	
	/*
	 * We will adjust stagnant listings in the following way:
	 *    IF current_margin > MARGIN_FLOOR
	 *       SET MARGIN = max(MARGIN_FLOOR, current_margin - MARGIN_ADJUSTMENT_STEP)
	 *    ELSE
	 *       FLAG LISTING FOR PURGE EXAMINATION      
	 */
	private final void adjustListing(final ListingDueForMarginAdjustment listing) {
		debug("Starting adjustment checks for: " + listing);
		if(listing.adjustmentType == MarginAdjustmentType.STAGNANT) {
			if(listing.currentMargin > MARGIN_FLOOR) {
				adjustMargin(listing);
			}
		} else { //productive listing
			adjustMargin(listing);
		}
	}
	
	private void adjustMargin(final ListingDueForMarginAdjustment listing) {
		final double newMargin = listing.adjustmentType == MarginAdjustmentType.STAGNANT 
				? Math.max(MARGIN_FLOOR, listing.currentMargin - MARGIN_ADJUSTMENT_STEP)
				: listing.currentMargin + MARGIN_ADJUSTMENT_STEP ;
		debug("Adjusting margin from " + listing.currentMargin + "% --> " + newMargin + "% for: " + listing);
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			final long ms = System.currentTimeMillis();
			st.execute("UPDATE marketplace_listing SET target_margin=" + newMargin + ", last_margin_update="+ms
					+ " WHERE id=" + listing.marketplaceListingId);
			System.out.println("\tsuccess.");
		} catch(final Exception e) {
			System.out.println("\tfailure.");
			e.printStackTrace();
		}
	}
	
	private void debug(final String str) {
		if(debug) {
			System.out.println("[MarginAdjuster] " + str);
		}
	}

}
