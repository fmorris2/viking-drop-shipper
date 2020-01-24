package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubGetPaymentIdRequest extends SamsClubRequest {
	
	private static final String URL = "https://www.samsclub.com/api/node/vivaldi/v1/account/wallet/cards?response_group=full";

	public SamsClubGetPaymentIdRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public Optional<JSONObject> execute() {
		final HttpRequestBase request = new HttpGet(URL);
		addHeaders(request);
		
		System.out.println("[SamsClubGetPaymentIdRequest] Dispatching GET request to " + URL);
		final Optional<String> responseStr = sendRequest(client, request, HttpStatus.SC_OK);
		if(responseStr.isPresent()) {
			System.out.println("[SamsClubGetPaymentIdRequest] Response: " + responseStr.get());
			return Optional.of(new JSONObject(responseStr));
		}
		
		return Optional.empty();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("accept", "application/json, text/plain, */*");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.5");
		request.addHeader("content-type", "application/json");
		request.addHeader("connection", "keep-alive");
		request.addHeader("host", "www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sp/checkout/?xid=cart:begin-checkout");
		request.addHeader("TE", "Trailers");
	}

}
