package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentListingStockEntry;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;

public class AliExpressFulfillmentStockChecker implements FulfillmentStockChecker {

	@Override
	public Optional<FulfillmentListingStockEntry> getStock(FulfillmentAccount account,
			FulfillmentListing fulfillmentListing) {
		return Optional.empty();
	}

}
