package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.utils;

import java.util.List;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRemoveFromCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public final class SamsClubCartUtils {
	
	private SamsClubCartUtils() {
		//utils class
	}
	
	public static boolean clearCart(final WrappedHttpClient client) {
		
		final SamsClubGetCartItemsRequest getCart = new SamsClubGetCartItemsRequest(client);
		final List<SamsClubCartItem> cartItems = getCart.execute();
		boolean cleared = true;
		
		final SamsClubRemoveFromCartRequest removeFromCartRequest = new SamsClubRemoveFromCartRequest(client);
		for(final SamsClubCartItem cartItem : cartItems) {
			if(!removeFromCartRequest.execute(cartItem)) {
				cleared = false;
			}
		}
		
		return cleared;
	}
	
}
