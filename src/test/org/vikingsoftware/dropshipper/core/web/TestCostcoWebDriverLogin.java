package test.org.vikingsoftware.dropshipper.core.web;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;

public class TestCostcoWebDriverLogin {

	@Test
	public void testSingleLogin() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAndRotateAccount(FulfillmentPlatforms.COSTCO);
		Assert.assertTrue(new CostcoWebDriver().getReady(account));
	}

	@Test
	public void testMultipleLogin() {
		testSingleLogin();
		testSingleLogin();
	}

}
