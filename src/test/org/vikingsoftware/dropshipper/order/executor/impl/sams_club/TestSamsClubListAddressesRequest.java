package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubListAddressesRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;

public class TestSamsClubListAddressesRequest {
	
	@Test
	public void test() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(15);
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		SamsClubSessionProvider.get().getSession(account, client);
		final SamsClubListAddressesRequest request = new SamsClubListAddressesRequest(client);
		final List<SamsClubAddress> addresses = request.execute();
		Assert.assertTrue(!addresses.isEmpty());
		System.out.println("Listed " + addresses.size() + " addresses.");
	}
}
