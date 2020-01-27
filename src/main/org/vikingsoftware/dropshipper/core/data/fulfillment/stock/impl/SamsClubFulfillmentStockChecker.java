package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentListingStockEntry;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubProductAPI;

public class SamsClubFulfillmentStockChecker implements FulfillmentStockChecker {

	private final SamsClubProductAPI api = new SamsClubProductAPI();
	
	@Override
	public Optional<FulfillmentListingStockEntry> getStock(FulfillmentAccount account, FulfillmentListing fulfillmentListing) {
		if(api.parse(fulfillmentListing.product_id)) {		
			if(!fulfillmentListing.item_id.equalsIgnoreCase(api.getItemNumber().orElse(null))) {
				System.out.println("Could not parse metadata as the item IDs don't match!");
				return Optional.of(new FulfillmentListingStockEntry(0, -1, -1));
			}
			
			final int stock = api.getAvailableToSellQuantity().orElse(0);
			final double price = api.getListPrice().orElse(-1D);
			final int minPurchaseQty = api.getMinPurchaseQty();
			return Optional.of(new FulfillmentListingStockEntry(stock, price, minPurchaseQty));
		}
		
		return Optional.empty();
	}
}
