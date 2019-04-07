package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;

public class FulfillmentStockManager {

	private FulfillmentStockManager(){}

	public static Future<Collection<SkuInventoryEntry>> getStock(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		FulfillmentAccount account = null;
		switch(FulfillmentPlatforms.getById(fulfillmentListing.fulfillment_platform_id)) {
			case ALI_EXPRESS:
				 account = FulfillmentAccountManager.get().peekAccount(FulfillmentPlatforms.ALI_EXPRESS);
				return AliExpressFulfillmentStockChecker.get().getStock(account, marketListing, fulfillmentListing);
			case SAMS_CLUB:
				account = FulfillmentAccountManager.get().peekAccount(FulfillmentPlatforms.SAMS_CLUB);
				return SamsClubFulfillmentStockChecker.get().getStock(account, marketListing, fulfillmentListing);
		}

		return new FutureTask<>(() -> Collections.emptyList());
	}
}
