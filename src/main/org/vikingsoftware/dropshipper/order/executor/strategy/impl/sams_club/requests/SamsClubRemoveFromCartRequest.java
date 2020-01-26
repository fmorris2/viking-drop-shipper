package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public class SamsClubRemoveFromCartRequest extends SamsClubRequest {

	private static final String URL_PREFIX = "https://www.samsclub.com/sams/cart/cartService.jsp?cartOperation=REMOVECARTITEM&cartItemIds=";
	private static final String URL_SUFFIX = "&ecomOrderToken=";
	
	public SamsClubRemoveFromCartRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public boolean execute(final SamsClubCartItem item) {
		final String url = URL_PREFIX + item.cartItemId + URL_SUFFIX + getCookie("samsorder");
		System.out.println("[SamsClubRemoveFromCartRequest] Formulating DELETE request for url: " + url);
		
		final HttpDelete request = new HttpDelete(url);
		addHeaders(request);
		return sendRequest(client, request, HttpStatus.SC_OK).isPresent();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("accept", "text/html, */*; q=0.01");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.5");
		request.addHeader("cache-control", "no-cache");
		request.addHeader("content-type", "application/json");
		request.addHeader("host", "www.samsclub.com");
		request.addHeader("origin", "https://www.samsclub.com");
		request.addHeader("pragma", "no-cache");
		request.addHeader("connection", "keep-alive");
		request.addHeader("referer", "https://www.samsclub.com/sams/cart/cart.jsp?xid=hdr_cart_view-cart-and-checkout");
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
	}
	
}
