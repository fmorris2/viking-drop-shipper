package test.org.vikingsoftware.dropshipper.core.web;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;

public class TestAliExpressWebDriverLogin {

	@Test
	public void testSingleLogin() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAndRotateEnabledAccount(FulfillmentPlatforms.ALI_EXPRESS);
		Assert.assertTrue(new AliExpressWebDriver().getReady(account));
	}

	@Test
	public void testMultipleLogin() {
		testSingleLogin();
		testSingleLogin();
	}

}
