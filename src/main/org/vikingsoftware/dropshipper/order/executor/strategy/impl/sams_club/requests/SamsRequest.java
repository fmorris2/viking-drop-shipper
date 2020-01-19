package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public abstract class SamsRequest {
	
	protected CookieStore cookies = new BasicCookieStore();
	
	protected WrappedHttpClient client;
	
	public SamsRequest(final WrappedHttpClient client, final CookieStore cookies) {
		this.client = client;
		if(cookies != null) {
			this.cookies = cookies;
		}
	}
	
	public WrappedHttpClient getClient() {
		return client;
	}
	
	public String getCookie(final String key) {
		return cookies.getCookies().stream()
				.filter(cookie -> key.equalsIgnoreCase(cookie.getName()))
				.map(cookie -> cookie.getValue())
				.findFirst()
				.orElse(null);
	}
	
	public CookieStore getCookieStore() {
		return cookies;
	}
	
	public void setCookieStore(final CookieStore cookies) {
		this.cookies = cookies;
	}
}
