package test.org.vikingsoftware.dropshipper.core.web;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class TestSamsClubWebDriverLogin {

	@Test
	public void testSingleLogin() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAndRotateEnabledAccount(FulfillmentPlatforms.SAMS_CLUB);
		final VSDSProxy proxy = HttpClientManager.get().getClient().proxy;
		Assert.assertTrue(new SamsClubWebDriver(proxy).getReady(account));
	}

	@Test
	public void testMultipleLogin() {
		testSingleLogin();
		testSingleLogin();
	}

}
