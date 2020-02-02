package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;

public class SamsClubRefreshSamsOrderRequest extends SamsClubRequest {
	
	private static final String URL = "https://www.samsclub.com/cartservice/v1/carts?response_groups=cart.medium";

	public SamsClubRefreshSamsOrderRequest(WrappedHttpClient client) {
		super(client);
	}
	
	public Optional<JSONObject> execute() {
		System.out.println("[SamsClubAddToCartRequest] Formulating POST request for url: " + URL);
		final HttpPost request = new HttpPost(URL);
		addHeaders(request);
		addPayload(request);
		
		final Optional<SamsClubResponse> response = sendRequest(client, request, HttpStatus.SC_OK);
		if(response.isPresent() && response.get().success) {
			return Optional.of(new JSONObject(response.get().response));
		}
		
		return Optional.empty();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("accept", "application/json");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.9,pt;q=0.8");
		request.addHeader("content-type", "application/json");
		request.addHeader("cache-control", "no-cache");
		request.addHeader("host", "www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sams/cart/cart.jsp?xid=hdr_cart_view-cart-and-checkout");
		request.addHeader("TE", "trailers");
		request.addHeader("wm_consumer.source_id", "2");
		request.addHeader("wm_tenant_id", "1");
		request.addHeader("wm_vertical_id", "3");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("X-Requested-With", "XMLHttpRequest");
	}
	
	private void addPayload(final HttpPost request) {
		final String json = constructPayloadJsonString();
		System.out.println("[SamsClubRefreshSamsOrderRequest] Constructing Payload w/ JSON: " + json);
		final StringEntity entity = new StringEntity(json,ContentType.APPLICATION_JSON);
		request.setEntity(entity);
	}
	
	public String constructPayloadJsonString() {
		final JSONObject jsonObj = new JSONObject();
		final JSONObject payload = new JSONObject();
		jsonObj.put("payload", payload);
		payload.put("customerId", "12345");
		final JSONArray storeFrontIds = new JSONArray();
		storeFrontIds.put("4969");
		payload.put("storeFrontIds", storeFrontIds);
		
		return jsonObj.toString();
	}
	

}
