package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.Optional;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCurrentContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;

public class TestSamsClubGetCurrentContractRequest extends SamsClubRequestTest {

	@Test
	public void test() {
		
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(15);
		SamsClubSessionProvider.get().getSession(account, client);
		
		final Optional<SamsClubAddress> defaultAddr = SamsClubAddress.findDefaultAddress(client);
		Assert.assertTrue(defaultAddr.isPresent());
		
		final SamsClubCreateContractRequest createContractRequest = new SamsClubCreateContractRequest(client, defaultAddr.get());
		Assert.assertTrue(createContractRequest.execute().isPresent());
		
		final SamsClubGetCurrentContractRequest getContractRequest = new SamsClubGetCurrentContractRequest(client);
		final Optional<JSONObject> response = getContractRequest.execute();
		Assert.assertTrue(response.isPresent());
	}
}
