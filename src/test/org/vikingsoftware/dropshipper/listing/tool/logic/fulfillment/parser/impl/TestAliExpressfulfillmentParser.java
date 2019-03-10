package test.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.AliExpressFulfillmentParser;

import org.junit.Assert;
import org.junit.Test;

public class TestAliExpressfulfillmentParser {

	@Test
	public void test() {
		final String url = "https://www.aliexpress.com/item/Coolreall-usb-cable-for-lightning-cable-Fast-Charging-Cable-iPhone-Charger-Cord-Usb-Data-Cable/32955700629.html";
		final Listing listing = AliExpressFulfillmentParser.get().getListingTemplate(url);
		Assert.assertNotNull(listing);
	}

}
