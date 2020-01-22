package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsPurchaseContractDependencies;

public class TestSamsClubGetCartRequest extends SamsRequestTest {
	
	@Test
	public void test() {
		System.out.println("Adding single item to cart...");
		final SamsClubItem item = new SamsClubItem("645081", "prod17750489", "sku18264565");
		final SamsClubAddToCartRequest addToCartReq = createAddToCartRequests(item).get(0);
		Assert.assertTrue(addToCartReq.execute());
		System.out.println("Testing get cart...");
		final SamsPurchaseContractDependencies dependencies = new SamsPurchaseContractDependencies(addToCartReq.getClient(), 
				addToCartReq.getCookieMap());
		final SamsClubGetCartItemsRequest request = new SamsClubGetCartItemsRequest(addToCartReq.getClient(), dependencies);
		Assert.assertTrue(!request.execute().isEmpty());
	}
}
