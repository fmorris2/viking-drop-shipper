package main.org.vikingsoftware.dropshipper.core.net.http;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;

import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;
import main.org.vikingsoftware.dropshipper.core.net.nord.NordVPNCredentialManager;


public final class HttpClientManager {
	
	private static final int CONNECTION_TTL_DAYS = 365;
	private static final int NUM_CONNECTIONS_BEFORE_CYCLE = 1000;
	
	// Format: us4112.nordvpn.com
	private static final String[] US_NORD_PROXY_IDS = {
		"4110", "4112", "4113", "4122", "4123", "4125",
		"4126"
	};
	
	private static HttpClientManager instance;
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Queue<WrappedHttpClient> clients = new LinkedList<>();
	
	private int numConnections = 0;
	
	private HttpClientManager() {
		//singleton can't be instantiated from the outside...
	}
	
	public static synchronized HttpClientManager get() {
		if(instance == null) {
			instance = new HttpClientManager();
			instance.populateClients();
		}
		
		return instance;
	}
	
	public WrappedHttpClient getClient() {
		lock.writeLock().lock();
		try {
			final WrappedHttpClient client = clients.peek();
			
			if(++numConnections >= NUM_CONNECTIONS_BEFORE_CYCLE) {
				System.out.println("Cycling Http Client due to Num Connections threshold");
				clients.add(clients.poll());
				numConnections = 0;
			}
			return client;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void flag(final WrappedHttpClient client) {
		lock.writeLock().lock();
		try {
			if(client == clients.peek()) {
				System.out.println("Rotating Http Client: " + clients.peek());
				clients.add(clients.poll());
				numConnections = 0;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void populateClients() {
		addNordClients();
		
		clients.add(generateHttpClientWithoutProxy());
	}
	
	private WrappedHttpClient generateHttpClientWithoutProxy() {
		final HttpClient client = HttpClients.custom()
				.setConnectionTimeToLive(CONNECTION_TTL_DAYS, TimeUnit.DAYS)
				.build();
		
		return new WrappedHttpClient(client, null);
	}
	
	private void addNordClients() {
		for(final String id : US_NORD_PROXY_IDS) {
			final String url = "us" + id + ".nordvpn.com";
			final VSDSProxy proxy = new VSDSProxy(url, 80, NordVPNCredentialManager.getUsername(),
					NordVPNCredentialManager.getPassword());
			System.out.println("Adding NordVPN proxy: " + proxy.host + ":" + proxy.port);
			
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(new AuthScope(proxy.host, proxy.port),
					new UsernamePasswordCredentials(proxy.user, proxy.pass));
			
			final HttpClient client = HttpClients.custom()
					.setConnectionTimeToLive(CONNECTION_TTL_DAYS, TimeUnit.DAYS)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setProxy(new HttpHost(proxy.host, proxy.port, HttpHost.DEFAULT_SCHEME_NAME))
					.build();
			
			clients.add(new WrappedHttpClient(client, proxy));
		}
	}
}
