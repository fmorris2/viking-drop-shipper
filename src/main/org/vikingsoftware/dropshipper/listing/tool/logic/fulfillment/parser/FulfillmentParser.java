package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public interface FulfillmentParser {

	public Listing getListingTemplate(final FulfillmentAccount account, final String url);

}
