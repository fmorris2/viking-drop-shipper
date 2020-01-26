package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubCreateSessionRequest extends SamsClubRequest {

	private static final String URL = "https://www.samsclub.com/sams/account/signin/createSession.jsp";
	
	public SamsClubCreateSessionRequest(WrappedHttpClient client) {
		super(client);
	}
	
	public boolean execute() {
		final HttpGet request = new HttpGet(URL);
		addHeaders(request);	
		return sendRequest(client, request, HttpStatus.SC_OK).isPresent();
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("accept", "text/css,*/*;q=0.1");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.5");
		request.addHeader("connection", "keep-alive");
		request.addHeader("host", "www.samsclub.com");
		request.addHeader("referer", "https://www.samsclub.com/sams/…dr_cart_view-cart-and-checkout");
		request.addHeader("TE", "Trailers");		
	}

}
