package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.util.EntityUtils;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public class SamsClubRemoveFromCartRequest extends SamsRequest {
	
	private static final String URL_PREFIX = "https://www.samsclub.com/sams/cart/cartService.jsp?cartOperation=REMOVECARTITEM&cartItemIds=";
	private static final String URL_SUFFIX = "&ecomOrderToken=";
	
	public SamsClubRemoveFromCartRequest(WrappedHttpClient client, CookieStore cookies) {
		super(client, cookies);
	}
	
	public boolean execute(final SamsClubCartItem... items) {
		final String itemParameters = constructItemParameterString(items);
		final String url = URL_PREFIX + itemParameters + URL_SUFFIX + getCookie("samsorder");
		System.out.println("[SamsClubRemoveFromCartRequest] Formulating DELETE request for url: " + url);
		final HttpDelete request = new HttpDelete(url);
		addHeaders(request);
		return sendRequest(client, request);
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
	
	private void addHeaders(final HttpDelete request) {
		request.addHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36");
		request.addHeader("content-type", "application/json");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("dnt", "1");
	}
	
	private boolean sendRequest(final WrappedHttpClient client, final HttpDelete request) {
		try {
			final HttpResponse response = client.execute(request, client.createContextFromCookies(cookies));
			System.out.println("[SamsClubRemoveFromCartRequest] Response: " + EntityUtils.toString(response.getEntity()));
			return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
		} catch(final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return false;
	}
	
}
