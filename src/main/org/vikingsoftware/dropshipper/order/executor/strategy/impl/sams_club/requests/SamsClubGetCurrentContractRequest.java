package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubGetCurrentContractRequest extends SamsClubRequest {

	private static final String URL_PREFIX = "https://www.samsclub.com/api/node/vivaldi/v1/contracts/";
	
	public SamsClubGetCurrentContractRequest(WrappedHttpClient client) {
		super(client);
	}
	
	public Optional<JSONObject> execute() {
		final String url = URL_PREFIX + getCookie("samsorder");
		final HttpRequestBase request = new HttpGet(url);
		System.out.println("[SamsClubGetCurrentContractRequest] About to dispatch GET request to " + url);
		addHeaders(request);
		
		final Optional<String> response = sendRequest(client, request, HttpStatus.SC_OK);
		if(response.isPresent()) {
			System.out.println("[SamsClubGetCurrentContractRequest] Response: " + response.get());
			return Optional.of(new JSONObject(response.get()));
		}
		
		return Optional.empty();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("accept", "application/json, text/plain, */*");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.9,pt;q=0.8");
		request.addHeader("content-type", "application/json");
		request.addHeader("dnt", "1");
		request.addHeader("host", "www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sp/checkout/?singleCX=true&xid=cart_begin-checkout");
		request.addHeader("TE", "trailers");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
	}

}
