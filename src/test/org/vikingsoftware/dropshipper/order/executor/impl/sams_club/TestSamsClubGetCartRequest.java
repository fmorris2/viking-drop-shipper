package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;

public class TestSamsClubGetCartRequest {
	
	@Test
	public void test() {
		System.out.println("Adding single item to cart...");
		final SamsClubAddToCartRequest addToCartReq = new TestSamsClubAddToCartRequest().testAddSingleItem();
		System.out.println("Testing get cart...");
		final SamsClubGetCartItemsRequest request = new SamsClubGetCartItemsRequest(addToCartReq.getClient(), addToCartReq.getSessionCookies());
		Assert.assertTrue(!request.execute().isEmpty());
	}
}
