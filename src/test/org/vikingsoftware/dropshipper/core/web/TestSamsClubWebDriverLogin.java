package test.org.vikingsoftware.dropshipper.core.web;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class TestSamsClubWebDriverLogin {

	@Test
	public void testSingleLogin() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAndRotateAccount(FulfillmentPlatforms.SAMS_CLUB);
		Assert.assertTrue(new SamsClubWebDriver().getReady(account));
	}

	@Test
	public void testMultipleLogin() {
		testSingleLogin();
		testSingleLogin();
	}

}
