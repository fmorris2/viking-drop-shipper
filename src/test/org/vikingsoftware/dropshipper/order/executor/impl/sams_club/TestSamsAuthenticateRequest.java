package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAuthenticateRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsPurchaseContractDependencies;

public class TestSamsAuthenticateRequest {
	
	@Test
	public void test() {
		final SamsClubDriverSupplier driver = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final Map<String, String> session = driver.getSession(FulfillmentAccountManager.get().getAccountById(15), client.proxy);
		final SamsPurchaseContractDependencies dependencies = new SamsPurchaseContractDependencies(client, session);
		final SamsClubAuthenticateRequest request = new SamsClubAuthenticateRequest(client,dependencies);
		Assert.assertTrue(request.execute());
	}
}
