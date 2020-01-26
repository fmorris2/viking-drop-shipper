package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;

public interface FulfillmentStockChecker {
	public Optional<FulfillmentListingStockEntry> getStock(final FulfillmentAccount account, 
			final FulfillmentListing fulfillmentListing);
}
