package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.HashMap;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public abstract class SamsRequest {
	
	protected final Map<String, String> sessionCookies = new HashMap<>();
	
	protected WrappedHttpClient client;
	
	public WrappedHttpClient getClient() {
		return client;
	}
	
	public Map<String, String> getSessionCookies() {
		return sessionCookies;
	}
}
