package test.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl.AliExpressFulfillmentParser;

public class TestAliExpressfulfillmentParser {

	@Test
	public void test() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.ALI_EXPRESS);
		final String url = "https://www.aliexpress.com/item/Coolreall-usb-cable-for-lightning-cable-Fast-Charging-Cable-iPhone-Charger-Cord-Usb-Data-Cable/32955700629.html";
		final Listing listing = new AliExpressFulfillmentParser().getListingTemplate(account, url);
		Assert.assertNotNull(listing);
	}

}
