package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;

public class TestSamsClubAddToCartRequest {

	@Test
	public void testAddSingleItem() {
		final SamsClubDriverSupplier driver = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		final Map<String, String> session = driver.getSession(FulfillmentAccountManager.get().peekEnabledAccount(FulfillmentPlatforms.SAMS_CLUB));
		final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
				.session(session)
				.productId("prod17750489")
				.skuId("sku18264565")
				.itemNumber("645081")
				.quantity(1)
				.build();
		
		Assert.assertTrue(request.execute());
	}
}
