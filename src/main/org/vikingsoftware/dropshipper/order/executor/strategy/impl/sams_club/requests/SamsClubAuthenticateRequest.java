package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubAuthenticateRequest extends SamsRequest {

	private static final String BASE_URL = "http://www.samsclub.com/soa/services/v1/user/authenticate?_=";
	
	public SamsClubAuthenticateRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public boolean execute() {
		final String url = BASE_URL + System.currentTimeMillis();
		final HttpPost request = new HttpPost(url);
		addHeaders(request);
		return sendRequest(client, request);
	}
	
	private void addHeaders(final HttpPost request) {
		request.addHeader("accept", "*/*");
		request.addHeader("accept-language", "en-US,en;q=0.9,pt;q=0.8");
		request.addHeader("origin", "https://www.samsclub.com");
		request.addHeader("sec-fetch-mode", "cors");
		request.addHeader("sec-fetch-site", "same-origin");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("dnt", "1");
		request.addHeader("content-type", "application/json");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("wm_consumer.id", getCookie("CID"));
		request.addHeader("wm_qos.correlation_id", "4333");
		request.addHeader("wm_svc.env", "prod");
		request.addHeader("wm_svc.name", "sams-api");
		request.addHeader("wm_svc.version", "1.0.0");
		request.addHeader("x-requested-with", "XMLHttpRequest");
	}
	
	private boolean sendRequest(final WrappedHttpClient client, final HttpPost request) {
		try {
			System.out.println("Cookies: " + this.getCookieMap());
			System.out.println("Headers: " + Arrays.toString(request.getAllHeaders()));
			final HttpResponse response = client.execute(request);
			final String responseStr = EntityUtils.toString(response.getEntity());
			System.out.println("[SamsClubGetCartItemsRequest] Response: " + responseStr);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return true;
			}
		} catch(final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return false;
	}

}
