package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public class FulfillmentStockManager {

	private FulfillmentStockManager(){}

	public static Optional<FulfillmentListingStockEntry> getStock(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		FulfillmentAccount account = null;
		try {
			switch(FulfillmentPlatforms.getById(fulfillmentListing.fulfillment_platform_id)) {
				case ALI_EXPRESS:
					 account = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.ALI_EXPRESS);
					return new AliExpressFulfillmentStockChecker().getStock(account, fulfillmentListing);
				case SAMS_CLUB:
					account = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.SAMS_CLUB);
					return new SamsClubFulfillmentStockChecker().getStock(account, fulfillmentListing);
				case COSTCO:
					account = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.COSTCO);
					return new CostcoFulfillmentStockChecker().getStock(account, fulfillmentListing);
				case AMAZON:
					break;
				default:
					break;
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return Optional.empty();
	}
}
