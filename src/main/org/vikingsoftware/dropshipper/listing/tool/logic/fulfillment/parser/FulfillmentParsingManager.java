package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.AliExpressFulfillmentParser;

public class FulfillmentParsingManager {
	
	private FulfillmentParsingManager() {
		
	}
	
	public static Listing parseListing(final String url) {
		if(url != null) {
			if(url.contains("aliexpress.com")) {
				return AliExpressFulfillmentParser.get().getListingTemplate(url);
			}
		}
		
		return null;
	}
}
