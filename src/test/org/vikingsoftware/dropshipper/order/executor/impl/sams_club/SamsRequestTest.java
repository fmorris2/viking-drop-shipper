package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;

public class SamsRequestTest {
	
	protected List<SamsClubAddToCartRequest> createAddToCartRequests(final SamsClubItem... items) {
		final List<SamsClubAddToCartRequest> requests = new ArrayList<>();
		final SamsClubDriverSupplier driver = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final Map<String, String> session = driver.getSession(FulfillmentAccountManager.get().getAccountById(3), client.proxy);
		for(final SamsClubItem item : items) {
			final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
					.productId(item.productId)
					.skuId(item.skuId)
					.itemNumber(item.itemNumber)
					.client(client)
					.quantity(1)
					.build();
			request.setCookies("samsclub.com", "/", session);
			requests.add(request);
		}
		
		return requests;
	}
}
