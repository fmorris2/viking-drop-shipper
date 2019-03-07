package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public interface FulfillmentParser {
	
	public Listing getListingTemplate(final String url);
	
}
