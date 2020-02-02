package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;

public abstract class SamsClubRequest {
	
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36";
	
	protected WrappedHttpClient client;
	
	public SamsClubRequest(final WrappedHttpClient client) {
		this.client = client;
	}
	
	public WrappedHttpClient getClient() {
		return client;
	}
	
	public Optional<SamsClubResponse> sendRequest(final WrappedHttpClient client, final HttpRequestBase request, final int expectedResponseCode) {
		HttpResponse response = null;
		try {
			System.out.println("Cookie String: " + getCookieString());
			response = client.execute(request);
			System.out.println("Response status: " + response.getStatusLine());
			if(response.getStatusLine().getStatusCode() == expectedResponseCode) {
				final String responseStr = EntityUtils.toString(response.getEntity());
				return responseStr == null || responseStr.isEmpty() 
						? Optional.of(new SamsClubResponse(expectedResponseCode, "success", true)) 
						: Optional.of(new SamsClubResponse(expectedResponseCode, responseStr, true));
			} else {
				System.out.println("Response Headers: " + Arrays.toString(response.getAllHeaders()));
				return Optional.of(new SamsClubResponse(response.getStatusLine().getStatusCode(), "failure", false));
			}
		} catch (final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		} finally {
			client.release(response);
		}
		
		return Optional.empty();
	}
	
	public String getCookie(final String key) {
		return client.getCookieStore().getCookies().stream()
				.filter(cookie -> key.equalsIgnoreCase(cookie.getName()))
				.map(cookie -> cookie.getValue())
				.findFirst()
				.orElse(null);
	}
	
	public Map<String, String> getCookieMap() {
		final Map<String, String> cookieMap = new HashMap<>();
		
		for(final Cookie cookie : client.getCookieStore().getCookies()) {
			cookieMap.put(cookie.getName(), cookie.getValue());
		}
		
		return cookieMap;
	}
	
	public String getCookieString() {
		String cookieStr = "";
		for(final Cookie cookie : client.getCookieStore().getCookies()) {
			cookieStr += cookie.getName()+"="+cookie.getValue()+"; ";
		}
		
		return cookieStr;
	}
	
	public void setCookies(final String domain, final String path, final Map<String,String> cookies) {
		client.setCookies(domain, path, cookies);
	}
}
