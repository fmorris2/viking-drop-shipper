package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public abstract class SamsRequest {
	
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36";
	
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
	
	public Map<String, String> getCookieMap() {
		final Map<String, String> cookieMap = new HashMap<>();
		
		for(final Cookie cookie : cookies.getCookies()) {
			cookieMap.put(cookie.getName(), cookie.getValue());
		}
		
		return cookieMap;
	}
	
	public CookieStore getCookieStore() {
		return cookies;
	}
	
	public void setCookieStore(final CookieStore cookies) {
		this.cookies = cookies;
	}
}
