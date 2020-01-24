package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.ArrayList;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubLoginResponse;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;

public class SamsRequestTest {
	
	protected List<SamsClubAddToCartRequest> createAddToCartRequests(final SamsClubItem... items) {
		final List<SamsClubAddToCartRequest> requests = new ArrayList<>();
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount acc = FulfillmentAccountManager.get().getAccountById(15);
		final SamsClubLoginResponse session = SamsClubSessionProvider.get().getSession(acc, client);
		for(final SamsClubItem item : items) {
			final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
					.productId(item.productId)
					.skuId(item.skuId)
					.itemNumber(item.itemNumber)
					.client(client)
					.quantity(1)
					.build();
			request.setCookies("samsclub.com", "/", session.cookies);
			requests.add(request);
		}
		
		return requests;
	}
}
