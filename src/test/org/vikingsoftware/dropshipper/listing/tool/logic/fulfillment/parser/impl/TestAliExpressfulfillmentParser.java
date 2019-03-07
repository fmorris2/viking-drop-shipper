package test.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.AliExpressFulfillmentParser;

import org.junit.Assert;
import org.junit.Test;

public class TestAliExpressfulfillmentParser {

	@Test
	public void test() {
		final String url = "https://www.aliexpress.com/item/Silicone-Pancake-Mold-4-7-10-Holes-Egg-Ring-Maker-Round-Heart-Pancake-Mold-Nonstick-Circular/32816061966.html";
		final Listing listing = AliExpressFulfillmentParser.get().getListingTemplate(url);
		Assert.assertNotNull(listing);
	}

}
