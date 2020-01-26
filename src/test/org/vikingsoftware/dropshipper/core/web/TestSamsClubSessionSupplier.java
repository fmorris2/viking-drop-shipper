package test.org.vikingsoftware.dropshipper.core.web;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubOrderDetailsAPI;

public class TestSamsClubSessionSupplier {

	@Test
	public void test() {
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount acc = FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.SAMS_CLUB);
		Map<String, String> session = SamsClubSessionProvider.get().getSession(acc, client).cookies;
		System.out.println("Session: " + session);
		Assert.assertTrue(!session.isEmpty());
	}
	
	@Test
	public void testSamsOrderDetailsAPI() {
		final String orderId = "3818501695";
		final SamsClubOrderDetailsAPI api = new SamsClubOrderDetailsAPI();
		Assert.assertTrue(api.parse(orderId));
		System.out.println("API Status: " + api.getAPIStatus().orElse(null));
		System.out.println("Order State: " + api.getOrderState().orElse(null));
		System.out.println("Tracking Number: " + api.getTrackingNumber().orElse(null));
		Assert.assertTrue(api.getAPIStatus().isPresent() && "SUCCESS".equals(api.getAPIStatus().get()));
	}
}
