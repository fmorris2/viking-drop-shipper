package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;

public class FulfillmentStockManager {
	
	private FulfillmentStockManager(){}
	
	public static Future<Collection<SkuInventoryEntry>> getStock(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		switch(FulfillmentPlatforms.getById(fulfillmentListing.fulfillment_platform_id)) {
			case ALI_EXPRESS:
				return AliExpressFulfillmentStockChecker.get().getStock(marketListing, fulfillmentListing);
		}
		
		return new FutureTask<>(() -> Collections.emptyList());
	}
}
