package main.org.vikingsoftware.dropshipper.pricing.margins;

import java.util.Collections;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public class MarginAdjuster implements CycleParticipant {
	
	private static final String STAGNANT_LISTING_QUERY = "SELECT marketplace_listing.id,listing_title,target_margin,creation_timestamp"
			+ " FROM marketplace_listing LEFT_JOIN processed_orders ON marketplace_listing.id WHERE active";
	
	private boolean debug = true;

	@Override
	public void cycle() {
		debug("Cycling...");
		
		final List<MarketplaceListing> stagnantListings = getStagnantListings();
		debug("There are currently " + stagnantListings.size() + " stagnant listings to adjust...");
		
		for(final MarketplaceListing listing : stagnantListings) {
			adjustListing(listing);
		}
	}
	
	/*
	 * A Listing is considered "Stagnant" if the following conditions are true:
	 *    - The listing is active
	 *    - The last margin update for the listing is > MARGIN_UPDATE_TIME_WINDOW
	 *    - The last order for the listing is > LAST_ORDER_TIME_WINDOW   	
	 */
	private final List<MarketplaceListing> getStagnantListings() {
		return Collections.emptyList();
	}
	
	/*
	 * We will adjust stagnant listings in the following way:
	 *    IF current_margin > MARGIN_FLOOR
	 *       SET MARGIN = max(MARGIN_FLOOR, current_margin - MARGIN_ADJUSTMENT_STEP)
	 *    ELSE
	 *       FLAG LISTING FOR PURGE EXAMINATION      
	 */
	private final void adjustListing(final MarketplaceListing listing) {
		debug("Adjusting stagnant listing w/ id [" + listing.id + "] and title [" + listing.listingTitle + "]");
	}
	
	private void debug(final String str) {
		if(debug) {
			System.out.println("[MarginAdjuster] " + str);
		}
	}

}
