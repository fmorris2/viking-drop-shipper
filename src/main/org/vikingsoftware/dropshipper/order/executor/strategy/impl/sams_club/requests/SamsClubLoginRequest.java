package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubLoginRequest extends SamsClubRequest {
	
	private static final String URL = "https://www.samsclub.com/api/node/vivaldi/v1/auth/login";

	private final FulfillmentAccount account;
	
	public SamsClubLoginRequest(final FulfillmentAccount account, final WrappedHttpClient client) {
		super(client);
		this.account = account;
	}
	
	public Optional<JSONObject> execute() {
		System.out.println("[SamsClubLoginRequest] About to dispatch POST request to URL: " + URL);
		try {
			final HttpPost request = new HttpPost(URL);
			addHeaders(request);
			addPayload(request);
			final String responseStr = sendRequest(client, request);
			if(responseStr != null) {
				return Optional.of(new JSONObject(responseStr));
			}
		} catch(final Exception e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return Optional.empty();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("content-type", "application/json");
		request.addHeader("accept", "application/json, text/plain, */*");
		request.addHeader("accept-language", "en-US,en;q=0.9,pt;q=0.8");
		request.addHeader("apikey", "Desktop");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("dnt", "1");
		request.addHeader("origin", "https://www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sams/account/signin/login.jsp");
		request.addHeader("sec-fetch-mode", "cors");
		request.addHeader("sec-fetch-site", "same-origin");
	}
	
	private void addPayload(final HttpPost request) {
		final String json = constructPayloadJsonString();
		System.out.println("[SamsClubLoginRequest] Constructing Payload w/ JSON: " + json);
		final StringEntity entity = new StringEntity(json,ContentType.APPLICATION_JSON);
		request.setEntity(entity);
	}
	
	private String constructPayloadJsonString() {
		final JSONObject jsonObj = new JSONObject();
		jsonObj.put("username", account.username);
		jsonObj.put("password", account.password);
		jsonObj.put("stayLog", true);
		jsonObj.put("deviceId", "");
		jsonObj.put("response_group", "member");
		jsonObj.put("prftcf", "undefined");
		return jsonObj.toString();
	}
	
	private String sendRequest(final WrappedHttpClient client, final HttpPost request) {
		String responseStr = null;
		try {
			final HttpResponse response = client.execute(request);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				responseStr = EntityUtils.toString(response.getEntity());
				System.out.println("[SamsClubLoginRequest] Response: " + responseStr);
			} else {
				System.out.println("[SamsClubLoginRequest] Request failed. Response: " + response.getStatusLine());
				HttpClientManager.get().flag(client);
			}
		} catch(final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return responseStr;
	}

}
