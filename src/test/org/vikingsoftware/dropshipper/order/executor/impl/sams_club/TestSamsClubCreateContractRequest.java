package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;

public class TestSamsClubCreateContractRequest {

	@Test
	public void test() {
		
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(15);
		SamsClubSessionProvider.get().getSession(account, client);
		
		final Optional<SamsClubAddress> defaultAddr = SamsClubAddress.findDefaultAddress(client);
		Assert.assertTrue(defaultAddr.isPresent());
		
		final SamsClubCreateContractRequest request = new SamsClubCreateContractRequest(client, defaultAddr.get());
		Assert.assertTrue(request.execute().isPresent());
	}
}
