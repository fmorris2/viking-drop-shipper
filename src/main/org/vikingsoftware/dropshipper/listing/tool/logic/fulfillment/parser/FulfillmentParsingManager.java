package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.AliExpressFulfillmentParser;

public class FulfillmentParsingManager {

	private static FulfillmentAccount aliExpressAccount = FulfillmentAccountManager.get().peekAccount(FulfillmentPlatforms.ALI_EXPRESS);

	private FulfillmentParsingManager() {

	}

	public static Listing parseListing(final String url) {
		if(url != null) {
			if(url.contains("aliexpress.com")) {
				return AliExpressFulfillmentParser.get().getListingTemplate(aliExpressAccount, url);
			}
		}

		return null;
	}
}
