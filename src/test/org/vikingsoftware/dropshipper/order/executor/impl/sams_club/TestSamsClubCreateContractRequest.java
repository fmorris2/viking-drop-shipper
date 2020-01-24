package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubLoginResponse;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsPurchaseContractDependencies;

public class TestSamsClubCreateContractRequest {

	@Test
	public void test() {
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(15);
		final SamsClubLoginResponse session = SamsClubSessionProvider.get().getSession(account, client);
		final SamsPurchaseContractDependencies dependencies = new SamsPurchaseContractDependencies(client, session.cookies);
		final SamsClubAddress address = new SamsClubAddress.Builder()
				.addressId(dependencies.address.addressId)
				.addressType("Residential")
				.firstName("BRENDAN")
				.lastName("ROSA")
				.addressLineOne("105 S D ST")
				.city("EASLEY")
				.stateOrProvinceCode("SC")
				.postalCode("29640")
				.countryCode("US")
				.phone("916-245-0125")
				.build();
		final SamsClubCreateContractRequest request = new SamsClubCreateContractRequest(client,dependencies, address);
		Assert.assertTrue(request.execute());
	}
}
