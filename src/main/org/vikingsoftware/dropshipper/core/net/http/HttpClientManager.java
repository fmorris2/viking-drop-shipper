package main.org.vikingsoftware.dropshipper.core.net.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import main.org.vikingsoftware.dropshipper.core.net.proxy.ProxyManager;
import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxy;


public final class HttpClientManager {
	
	private static final int MAX_CONCURRENT_CONNECTIONS_PER_CLIENT = 100;
	
	private static HttpClientManager instance;
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Queue<WrappedHttpClient> clients = new LinkedList<>();
	
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
			return client;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public WrappedHttpClient getAndRotateClient() {
		lock.writeLock().lock();
		try {			
			final WrappedHttpClient client = clients.poll();
			clients.add(client);
			
			return client;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void rotateClient() {
		lock.writeLock().lock();
		try {		
			clients.add(clients.poll());
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void flag(final WrappedHttpClient client) {
		lock.writeLock().lock();
		try {
			if(client == clients.peek()) {
				System.out.println("Rotating Http Client: " + clients.peek());
				client.resetContext();
				clients.add(clients.poll());
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void populateClients() {
		addProxiedClients();
		
		clients.add(generateHttpClientWithoutProxy());
	}
	
	private WrappedHttpClient generateHttpClientWithoutProxy() {
		final HttpClient client = HttpClients.custom()
				.setConnectionManager(generateConnectionManager())
				.build();

		return new WrappedHttpClient(client, null);
	}
	
	private void addProxiedClients() {
		final List<VSDSProxy> proxies = new ArrayList<>(ProxyManager.get().getAllProxies());
		Collections.shuffle(proxies);
		for (final VSDSProxy proxy : proxies) {
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(new AuthScope(proxy.host, proxy.httpsPort),
					new UsernamePasswordCredentials(proxy.authenticationAccount.username, proxy.authenticationAccount.password));

			final HttpClient client = HttpClients.custom()
					.setProxy(new HttpHost(proxy.host, proxy.httpsPort))
					.setConnectionManager(generateConnectionManager())
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();
			
			final WrappedHttpClient wrappedClient = new WrappedHttpClient(client, proxy);
			
			clients.add(wrappedClient);
		}
	}
	
	private HttpClientConnectionManager generateConnectionManager() {
		final PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
		manager.setMaxTotal(MAX_CONCURRENT_CONNECTIONS_PER_CLIENT);
		manager.setDefaultMaxPerRoute(MAX_CONCURRENT_CONNECTIONS_PER_CLIENT);
		return manager;
	}
}
