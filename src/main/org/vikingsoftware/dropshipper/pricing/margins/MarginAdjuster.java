package main.org.vikingsoftware.dropshipper.pricing.margins;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
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
	
	private static final double MARGIN_FLOOR = 5.0; //5% margin is the lowest we'll go
	private static final double MARGIN_ADJUSTMENT_STEP = 1.0; //We'll lower the margin by this amount on each adjustment
	
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
			" WHERE marketplace_listing.active" + 
			" AND (UNIX_TIMESTAMP() * 1000) - marketplace_listing.last_margin_update > " + LAST_MARGIN_UPDATE_TIME_WINDOW;
	
	private boolean debug = true;
	
	public static void main(final String[] args) {
		System.out.println(STAGNANT_LISTING_QUERY);
	}

	@Override
	public void cycle() {
		debug("Cycling...");
		
		final List<StagnantListing> stagnantListings = getStagnantListings();
		debug("There are currently " + stagnantListings.size() + " stagnant listings to adjust...");
		
		for(final StagnantListing listing : stagnantListings) {
			adjustListing(listing);
		}
	}
	
	/*
	 * A Listing is considered "Stagnant" if the following conditions are true:
	 *    - The listing is active
	 *    - The last margin update for the listing is > MARGIN_UPDATE_TIME_WINDOW
	 *    - The last order for the listing is > LAST_ORDER_TIME_WINDOW   	
	 */
	private final List<StagnantListing> getStagnantListings() {
		final List<StagnantListing> stagnantListings = new ArrayList<>();
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery(STAGNANT_LISTING_QUERY);
			while(res.next()) {
				final StagnantListing listing = new StagnantListing(res.getInt("stagnantMarketplaceListingID"),
						res.getDouble("currentMargin"), res.getString("listingTitle"));
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
	private final void adjustListing(final StagnantListing listing) {
		debug("Starting adjustment checks for: " + listing);
		if(listing.currentMargin > MARGIN_FLOOR) {
			decreaseMargin(listing);
		} else { //flag listing for purge examination
			flagForPurgeExamination(listing);
		}
	}
	
	private void decreaseMargin(final StagnantListing listing) {
		final double newMargin = Math.max(MARGIN_FLOOR, listing.currentMargin - MARGIN_ADJUSTMENT_STEP);
		debug("Adjusting margin from " + listing.currentMargin + "% --> " + newMargin + "% for: " + listing);
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			final long ms = System.currentTimeMillis();
			st.execute("UPDATE marketplace_listing SET target_margin=" + newMargin + ", last_margin_update="+ms
					+ " WHERE id=" + listing.marketplaceListingId);
			System.out.println("\tsuccess.");
		} catch(final Exception e) {
			System.out.println("\tfailure.");
			e.printStackTrace();
		}
	}
	
	private void flagForPurgeExamination(final StagnantListing listing) {
		debug("Flagging for purge examination: " + listing);
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("UPDATE marketplace_listing SET needs_purge_examination=1"
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
