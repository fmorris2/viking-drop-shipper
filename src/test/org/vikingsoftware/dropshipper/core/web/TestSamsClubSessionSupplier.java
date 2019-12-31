package test.org.vikingsoftware.dropshipper.core.web;

import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsOrderDetailsAPI;

public class TestSamsClubSessionSupplier {

	@Test
	public void test() {
		final SamsClubDriverSupplier supplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		if(supplier != null) {
			final FulfillmentAccount acc = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.SAMS_CLUB);
			Map<String, String> session = supplier.getSession(acc);
			System.out.println("Session: " + session);
			Assert.assertTrue(!session.isEmpty());
			supplier.clearSession(acc);
			System.out.println("Session has been cleared.");
			System.out.println("Attempting to grab session again...");
			session = supplier.getSession(acc);
			System.out.println("Second session: " + session);
			Assert.assertTrue(!session.isEmpty());	
		} else {
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testSamsOrderDetailsAPI() {
		final String orderId = "3818501695";
		final SamsOrderDetailsAPI api = new SamsOrderDetailsAPI();
		Assert.assertTrue(api.parse(orderId));
		System.out.println("API Status: " + api.getAPIStatus().orElse(null));
		System.out.println("Order State: " + api.getOrderState().orElse(null));
		System.out.println("Tracking Number: " + api.getTrackingNumber().orElse(null));
		Assert.assertTrue(api.getAPIStatus().isPresent() && "SUCCESS".equals(api.getAPIStatus().get()));
	}
}
