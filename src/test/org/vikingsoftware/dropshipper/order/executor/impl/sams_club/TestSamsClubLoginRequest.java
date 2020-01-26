package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubLoginRequest;

public class TestSamsClubLoginRequest extends SamsClubRequestTest {
	
	@Test
	public void testSingleLogin() {
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(15);
		System.out.println("Testing single login on Sams Club w/ account " + account.username);
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final SamsClubLoginRequest request = new SamsClubLoginRequest(account, client);
		Assert.assertTrue(request.execute().isPresent());	
	}
}
