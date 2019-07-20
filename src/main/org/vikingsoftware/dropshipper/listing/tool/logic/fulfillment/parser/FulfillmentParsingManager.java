package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.AliExpressFulfillmentParser;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.CostcoFulfillmentParser;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.SamsClubFulfillmentParser;

public class FulfillmentParsingManager {

	private static final FulfillmentAccount aliExpressAccount = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.ALI_EXPRESS);
	private static final FulfillmentAccount costcoAccount = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.COSTCO);
	private static final FulfillmentAccount samsClubAccount = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.SAMS_CLUB);
	
	private FulfillmentParsingManager() {

	}

	public static Listing parseListing(final String url) {
		if(url != null) {
			if(url.contains("aliexpress.com")) {
				return new AliExpressFulfillmentParser().getListingTemplate(aliExpressAccount, url);
			}

			if(url.contains("costco.com")) {
				return new CostcoFulfillmentParser().getListingTemplate(costcoAccount, url);
			}
			
			if(url.contains("samsclub")) {
				return new SamsClubFulfillmentParser().getListingTemplate(samsClubAccount, url);
			}
		}

		return null;
	}
}
