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
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;

public class TestSamsClubGetCartRequest {
	
	@Test
	public void test() {
		System.out.println("Adding single item to cart...");
		final SamsClubAddToCartRequest addToCartReq = createAddToCartRequest();
		Assert.assertTrue(addToCartReq.execute());
		System.out.println("Testing get cart...");
		final SamsClubGetCartItemsRequest request = new SamsClubGetCartItemsRequest(addToCartReq.getClient(), addToCartReq.getCookieStore());
		Assert.assertTrue(!request.execute().isEmpty());
	}
	
	private SamsClubAddToCartRequest createAddToCartRequest() {
		final SamsClubDriverSupplier driver = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		final Map<String, String> session = driver.getSession(FulfillmentAccountManager.get().getAccountById(3));
		final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
				.cookies(WrappedHttpClient.createCookieStoreFromMap(session))
				.productId("prod17750489")
				.skuId("sku18264565")
				.itemNumber("645081")
				.client(HttpClientManager.get().getClient())
				.quantity(1)
				.build();
		
		return request;
	}
}
