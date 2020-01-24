package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;

public class SamsClubCreateContractRequest extends SamsClubRequest {
	
	private static final String URL_PREFIX = "https://www.samsclub.com/vapi/v1/create-contract/";
	
	private final SamsClubAddress address;
	
	public SamsClubCreateContractRequest(WrappedHttpClient client, final SamsClubAddress address) {
		super(client);
		this.address = address;
	}
	
	public Optional<JSONObject> execute() {
		final String url = URL_PREFIX + getCookie("samsorder");
		System.out.println("[SamsClubCreateContractRequest] Dispatching POST to " + url);
		final HttpPost request = new HttpPost(url);
		addHeaders(request);
		addPayload(request);
		final Optional<String> response = sendRequest(client, request, HttpStatus.SC_OK);
		if(response.isPresent()) {
			System.out.println("[SamsClubCreateContractRequest] Response: " + response.get());
			return Optional.of(new JSONObject(response.get()));
		}
		
		return Optional.empty();
	}
	
	private void addPayload(final HttpPost request) {
		final String json = constructPayloadJsonString();
		System.out.println("[SamsClubCreateContractRequest] Constructing Payload w/ JSON: " + json);
		final StringEntity entity = new StringEntity(json,ContentType.APPLICATION_JSON);
		request.setEntity(entity);
	}
	
	private String constructPayloadJsonString() {
		final JSONObject json = new JSONObject();
		final JSONObject payload = new JSONObject();
		payload.put("autocorrect", true);
		final JSONObject location = new JSONObject();
		address.updateJSONObject(location);
		
		json.put("payload", payload);	
		payload.put("autocorrect", true);
		payload.put("prftcf", "undefined");
		payload.put("location", location);
		
		return json.toString();
	}
	
	private void addHeaders(final HttpPost request) {
		request.addHeader("accept", "application/json");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.9,pt;q=0.8");
		request.addHeader("cache-control", "no-cache");
		request.addHeader("content-type", "application/json");
		request.addHeader("dnt", "1");
		request.addHeader("jsessionid", getCookie("JSESSIONID"));
		request.addHeader("origin", "https://www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sams/cart/cart.jsp?xid=pdp_view-cart&eventId=scCheckout&sortProperties=-lastModifiedDate");
		request.addHeader("sec-fetch-mode", "cors");
		request.addHeader("sec-fetch-site", "same-origin");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
	}

}
