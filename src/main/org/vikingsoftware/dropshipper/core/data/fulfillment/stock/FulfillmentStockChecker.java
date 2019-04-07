package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

import java.util.Collection;
import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;

public interface FulfillmentStockChecker {
	public Future<Collection<SkuInventoryEntry>> getStock(final FulfillmentAccount account,
			final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing);
}
