package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.List;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public class SamsClubRemoveFromCartRequest extends SamsRequest {
	
	private final List<SamsClubCartItem> items;
	
	public SamsClubRemoveFromCartRequest(final List<SamsClubCartItem> items, final Map<String, String> cookies) {
		this.sessionCookies.putAll(cookies);
		this.items = items;
	}
	
}
