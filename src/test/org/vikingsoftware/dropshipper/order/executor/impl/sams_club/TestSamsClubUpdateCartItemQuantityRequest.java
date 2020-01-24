package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubUpdateCartItemQuantityRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;

public class TestSamsClubUpdateCartItemQuantityRequest extends SamsClubRequestTest {
	
	private static final int UPDATE_AMT = 5;
	
	@Test
	public void testUpdateSingleItem() {
		System.out.println("Adding single item to cart...");
		final SamsClubItem item = new SamsClubItem("645081", "prod17750489", "sku18264565");
		final SamsClubAddToCartRequest addToCartReq = createAddToCartRequests(item).get(0);
		Assert.assertTrue(addToCartReq.execute());
		
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(addToCartReq.getClient());
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		Assert.assertTrue(!currentCartItems.isEmpty());
		
		System.out.println("Testing update of item quantity in cart...");
		final SamsClubUpdateCartItemQuantityRequest updateReq = new SamsClubUpdateCartItemQuantityRequest(addToCartReq.getClient());
		Assert.assertTrue(updateReq.execute(currentCartItems.stream()
				.map(itm -> new SamsClubCartItem.Builder()
						.cartItemId(itm.cartItemId)
						.quantity(UPDATE_AMT)
						.build()
					)
				.toArray(SamsClubCartItem[]::new)));
		
		final SamsClubGetCartItemsRequest getCartReq2 = new SamsClubGetCartItemsRequest(addToCartReq.getClient());
		final List<SamsClubCartItem> updatedCartItems = getCartReq2.execute();
		
		Assert.assertTrue(!updatedCartItems.isEmpty() &&
				updatedCartItems.stream().allMatch(itm -> itm.quantity == UPDATE_AMT));
	}
}
