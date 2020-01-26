package test.org.vikingsoftware.dropshipper.http;

import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class TestHttpClientProxies {
	
	private static final String WEBHOOK_URL = "https://webhook.site/23c05081-0183-4d2f-9b71-216ce73653b9";
	
	@Test
	public void test() {
		
		final Set<WrappedHttpClient> clientsTested = new HashSet<>();
		
		WrappedHttpClient client = HttpClientManager.get().getClient();
		while(!clientsTested.contains(client)) {
			
			final HttpGet req = new HttpGet(WEBHOOK_URL);
			try {
				client.execute(req);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			
			clientsTested.add(client);
			HttpClientManager.get().flag(client);
			client = HttpClientManager.get().getClient();
		}
		
	}
}
