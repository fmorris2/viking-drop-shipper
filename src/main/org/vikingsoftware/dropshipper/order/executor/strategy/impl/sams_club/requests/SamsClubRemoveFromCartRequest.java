package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.List;

import org.apache.http.client.CookieStore;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public class SamsClubRemoveFromCartRequest extends SamsRequest {
	
	private final List<SamsClubCartItem> items;
	
	public SamsClubRemoveFromCartRequest(final WrappedHttpClient client, final CookieStore cookies,
			final List<SamsClubCartItem> items) {
		super(client, cookies);
		this.items = items;
	}
	
}
