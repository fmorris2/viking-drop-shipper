package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentListingStockEntry;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubProductAPI;

public class SamsClubFulfillmentStockChecker implements FulfillmentStockChecker {

	public static final double SAMS_CLUB_SHIPPING_RATE = 5.00; //$5
	
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
			double price = api.getListPrice().orElse(-1D);
			final int minPurchaseQty = api.getMinPurchaseQty();
			final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
					.successfullyParsedDetails(true)
					.minPurchaseQty(minPurchaseQty)
					.stock(stock)
					.price((price * minPurchaseQty) + SAMS_CLUB_SHIPPING_RATE)
					.build();
			return Optional.of(entry);
		}
		
		return Optional.empty();
	}
}
