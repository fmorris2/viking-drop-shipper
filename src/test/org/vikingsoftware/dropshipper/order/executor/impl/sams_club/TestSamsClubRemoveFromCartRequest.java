package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRemoveFromCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;

public class TestSamsClubRemoveFromCartRequest extends SamsClubRequestTest {
	
	@Test
	public void testRemoveSingleItem() {
		System.out.println("Adding single item to cart...");
		final SamsClubItem item = new SamsClubItem("645081", "prod17750489", "sku18264565");
		final SamsClubAddToCartRequest addToCartReq = createAddToCartRequests(item).get(0);
		
		final Optional<SamsClubResponse> addToCartResponse = addToCartReq.execute();
		Assert.assertTrue(addToCartResponse.isPresent() && addToCartResponse.get().success);
		
		System.out.println("Testing get cart...");
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(addToCartReq.getClient());
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		Assert.assertTrue(!currentCartItems.isEmpty());
		
		System.out.println("Testing removal of item from cart...");
		final SamsClubRemoveFromCartRequest removeFromCartReq = new SamsClubRemoveFromCartRequest(addToCartReq.getClient());
		Assert.assertTrue(removeFromCartReq.execute(currentCartItems.get(0)));
	}
}
