package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubLoginResponse;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;

public class TestSamsClubAddToCartRequest {

	@Test
	public void testAddSingleItem() {
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount acc = FulfillmentAccountManager.get().getAccountById(15);
		final SamsClubLoginResponse session = SamsClubSessionProvider.get().getSession(acc, client);
		final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
				.productId("prod17750489")
				.skuId("sku18264565")
				.itemNumber("645081")
				.client(client)
				.quantity(1)
				.build();
		
		final Optional<SamsClubResponse> response = request.execute();
		
		Assert.assertTrue(response.isPresent() && response.get().success);
	}
}
