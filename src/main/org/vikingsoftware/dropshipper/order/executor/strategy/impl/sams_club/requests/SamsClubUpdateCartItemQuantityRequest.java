package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;

public class SamsClubUpdateCartItemQuantityRequest extends SamsClubRequest {

	private static final String URL = "https://www.samsclub.com/sams/cart/cartService.jsp?cartOperation=UPDATECARTITEM&ecomOrderToken=";
	
	public SamsClubUpdateCartItemQuantityRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public boolean execute(final SamsClubCartItem... items) {
		final String url = URL + getCookie("samsorder");
		System.out.println("[SamsClubUpdateCartItemQuantityRequest] Formulating PUT request for url: " + url);
		
		final HttpPut request = new HttpPut(url);
		addHeaders(request);
		addPayload(request, items);
		return sendRequest(client, request, HttpStatus.SC_OK).isPresent();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("accept", "text/html, */*; q=0.01");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.5");
		request.addHeader("content-type", "application/json");
		request.addHeader("dnt", "1");
		request.addHeader("origin", "https://www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sams/cart/cart.jsp?xid=hdr_checkout_back-to-cart");
		request.addHeader("sec-fetch-mode", "cors");
		request.addHeader("sec-fetch-site", "same-origin");
		request.addHeader("pragma", "no-cache");
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
	}
	
	private void addPayload(final HttpPut request, final SamsClubCartItem... items) {
		final String json = constructPayloadJsonString(items);
		System.out.println("[SamsClubUpdateCartItemQuantityRequest] Constructing Payload w/ JSON: " + json);
		final StringEntity entity = new StringEntity(json,ContentType.APPLICATION_JSON);
		request.setEntity(entity);
	}
	
	private String constructPayloadJsonString(final SamsClubCartItem... items) {
		final JSONObject json = new JSONObject();
		final JSONObject payload = new JSONObject();
		json.put("payload", payload);
		
		final JSONArray itemsArr = new JSONArray();
		payload.put("items", itemsArr);
		for(final SamsClubCartItem item : items) {
			final JSONObject itemObj = new JSONObject();
			itemObj.put("id", item.cartItemId);
			itemObj.put("quantity", item.quantity);
			itemsArr.put(itemObj);
		}
		
		return json.toString();
	}

}
