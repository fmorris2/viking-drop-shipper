package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsPurchaseContractDependencies;

public class SamsClubAuthenticateRequest extends SamsRequest {

	private static final String BASE_URL = "http://www.samsclub.com/soa/services/v1/user/authenticate?_=";
	
	private final SamsPurchaseContractDependencies dependencies;
	
	public SamsClubAuthenticateRequest(final WrappedHttpClient client, final CookieStore cookies,
			final SamsPurchaseContractDependencies dependencies) {
		super(client, cookies);
		this.dependencies = dependencies;
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
		request.addHeader("wm_consumer.id", dependencies.getMapping("wm_consumer.id"));
		request.addHeader("wm_qos.correlation_id", dependencies.getMapping("wm_qos.correlation_id"));
		request.addHeader("wm_svc.env", dependencies.getMapping("wm_svc.env"));
		request.addHeader("wm_svc.name", dependencies.getMapping("wm_svc.name"));
		request.addHeader("wm_svc.version", dependencies.getMapping("wm_svc.version"));
		request.addHeader("x-requested-with", "XMLHttpRequest");
	}
	
	private boolean sendRequest(final WrappedHttpClient client, final HttpPost request) {
		try {
			System.out.println("Cookies: " + this.getCookieMap());
			System.out.println("Headers: " + Arrays.toString(request.getAllHeaders()));
			final HttpResponse response = client.execute(request, client.createContextFromCookies(cookies));
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
