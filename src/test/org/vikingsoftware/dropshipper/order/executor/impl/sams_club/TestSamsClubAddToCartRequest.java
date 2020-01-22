package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;

public class TestSamsClubAddToCartRequest {

	@Test
	public void testAddSingleItem() {
		final SamsClubDriverSupplier driver = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final Map<String, String> session = driver.getSession(FulfillmentAccountManager.get().getAccountById(3), client.proxy);
		final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
				.productId("prod17750489")
				.skuId("sku18264565")
				.itemNumber("645081")
				.client(client)
				.quantity(1)
				.build();
		
		request.setCookies("samsclub.com", "/", session);
		Assert.assertTrue(request.execute());
	}
}
