package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public class SamsClubRemoveFromCartRequest extends SamsRequest {

	private static final String URL_PREFIX = "https://www.samsclub.com/sams/cart/cartService.jsp?cartOperation=REMOVECARTITEM&cartItemIds=";
	private static final String URL_SUFFIX = "&ecomOrderToken=";
	
	public SamsClubRemoveFromCartRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public boolean execute(final SamsClubCartItem... items) {
		final String itemParameters = constructItemParameterString(items);
		final String url = URL_PREFIX + itemParameters + URL_SUFFIX + getCookie("samsorder");
		System.out.println("[SamsClubRemoveFromCartRequest] Formulating DELETE request for url: " + url);
		
		final HttpDelete request = new HttpDelete(url);
		addHeaders(request);
		return sendRequest(client, request, HttpStatus.SC_OK);
	}
	
	private String constructItemParameterString(final SamsClubCartItem... items) {
		String parameterString = "";
		for(int i = 0; i < items.length; i++) {
			parameterString += items[i].cartItemId;
			if(i < items.length - 1) {
				parameterString += ",";
			}
		}
		
		return parameterString;
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
		request.addHeader("user-agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:72.0) Gecko/20100101 Firefox/72.0");
	}
	
	private boolean sendRequest(final WrappedHttpClient client, final HttpRequestBase request, final int expectedResponseCode) {
		try {
			System.out.println("Cookie String: " + getCookieString());
			final HttpResponse response = client.execute(request);
			System.out.println("Response status: " + response.getStatusLine());
			return response.getStatusLine().getStatusCode() == expectedResponseCode;
		} catch(final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return false;
	}
	
}
