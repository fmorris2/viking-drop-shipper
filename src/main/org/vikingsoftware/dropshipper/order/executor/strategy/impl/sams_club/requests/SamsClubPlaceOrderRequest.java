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
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubPlaceOrderRequestDependencies;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;

public class SamsClubPlaceOrderRequest extends SamsClubRequest {
	
	private static final String URL_PREFIX = "https://www.samsclub.com/api/node/vivaldi/v1/express-placeorder/";
	private static final String URL_SUFFIX = "?source=2";

	private final SamsClubPlaceOrderRequestDependencies dependencies;
	
	public SamsClubPlaceOrderRequest(final WrappedHttpClient client, final SamsClubPlaceOrderRequestDependencies dependencies) {
		super(client);
		this.dependencies = dependencies;
	}
	
	public Optional<JSONObject> execute() {
		final String url = URL_PREFIX + getCookie("samsorder") + URL_SUFFIX;
		System.out.println("[SamsClubPlaceOrderRequest] About to dispatch POST request to " + url);
		final HttpPost request = new HttpPost(url);
		
		addHeaders(request);
		addPayload(request);
		
		final Optional<SamsClubResponse> response = sendRequest(client, request, HttpStatus.SC_OK);
		if(response.isPresent()) {
			System.out.println("[SamsClubPlaceOrderRequest] Response: " + response.get());
			return Optional.of(new JSONObject(response.get().response));
		}
		
		return Optional.empty();
	}
	
	private void addPayload(final HttpPost request) {
		final String json = constructPayloadJsonString();
		System.out.println("[SamsClubPlaceOrderRequest] Constructing Payload w/ JSON: " + json);
		final StringEntity entity = new StringEntity(json,ContentType.APPLICATION_JSON);
		request.setEntity(entity);
	}
	
	private String constructPayloadJsonString() {
		final JSONObject json = new JSONObject();
		final JSONArray paymentsArr = new JSONArray();
		json.put("payments", paymentsArr);
		
		final JSONObject paymentObj = new JSONObject();
		paymentObj.put("paymentId", dependencies.paymentId);
		paymentObj.put("amount", dependencies.amount);
		paymentObj.put("type", "creditCard");
		paymentsArr.put(paymentObj);
		
		json.put("prftcf", "undefined");
		
		return json.toString();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("accept", "application/json, text/plain, */*");
		request.addHeader("accept-language", "en-US,en;q=0.5");
		request.addHeader("referer", "https://www.samsclub.com/sp/checkout/?xid=cart:begin-checkout");
		request.addHeader("content-type", "application/json");
		request.addHeader("origin", "https://www.samsclub.com");
		request.addHeader("connection", "keep-alive");
	}

}
