package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.cookie.Cookie;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public abstract class SamsRequest {
	
	public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36";
	
	protected WrappedHttpClient client;
	
	public SamsRequest(final WrappedHttpClient client) {
		this.client = client;
	}
	
	public WrappedHttpClient getClient() {
		return client;
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
