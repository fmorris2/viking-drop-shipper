package main.org.vikingsoftware.dropshipper.core.net.http;

import java.net.Authenticator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import main.org.vikingsoftware.dropshipper.core.net.proxy.ProxyAuthenticator;
import main.org.vikingsoftware.dropshipper.core.net.proxy.ProxyManager;
import main.org.vikingsoftware.dropshipper.core.net.proxy.ProxySourceCooldownManager;
import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxy;
import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxySource;


public final class HttpClientManager {

	private static final SocksConnectionSocketFactory SOCKS_CONNECTION_MANAGER 
		= new SocksConnectionSocketFactory(SSLContexts.createSystemDefault());
	
	private static HttpClientManager instance;
	
	private final ProxySourceCooldownManager proxyCooldownManager = new ProxySourceCooldownManager();
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
			cycleClientsUntilNonCooldownProxyIsFound();		
			final WrappedHttpClient client = clients.peek();
			
			return client;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public WrappedHttpClient getAndRotateClient() {
		lock.writeLock().lock();
		try {		
			cycleClientsUntilNonCooldownProxyIsFound();		
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
	
	private void cycleClientsUntilNonCooldownProxyIsFound() {
		lock.writeLock().lock();
		try {
			WrappedHttpClient client = clients.peek();
			while(client.proxy != null && proxyCooldownManager.isOnCooldown(client.proxy.source)) {
				clients.add(clients.poll());
				client = clients.peek();
			}
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
	
	public void reportFailedProxyConnectionAttempt(final VSDSProxySource source) {
		proxyCooldownManager.reportFailedConnectionAttempt(source);
	}
	
	private void populateClients() {
		Authenticator.setDefault(new ProxyAuthenticator());
		//addProxiedClients();
		
		clients.add(generateHttpClientWithoutProxy());
	}
	
	private WrappedHttpClient generateHttpClientWithoutProxy() {
		final HttpClient client = HttpClients.custom()
				.setConnectionManager(new BasicHttpClientConnectionManager())
				.build();

		return new WrappedHttpClient(client, null);
	}
	
	private HttpClientConnectionManager generateSocksConnectionManager() {
		final Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", PlainConnectionSocketFactory.INSTANCE)
		        .register("https", SOCKS_CONNECTION_MANAGER)
		        .build();
		final HttpClientConnectionManager cm = new BasicHttpClientConnectionManager(reg);
		return cm;
	}
	
	private void addProxiedClients() {
		final List<VSDSProxy> proxies = new ArrayList<>(ProxyManager.get().getAllProxies());
		Collections.shuffle(proxies);
		for (final VSDSProxy proxy : proxies) {
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			//only socks proxies currently
			if(proxy.supportsSocks()) {
				credentialsProvider.setCredentials(new AuthScope(proxy.host, proxy.httpPort),
						new UsernamePasswordCredentials(proxy.authenticationAccount.username, proxy.authenticationAccount.password));
	
				final HttpClient client = HttpClients.custom()
						.setConnectionManager(generateSocksConnectionManager())
						.build();
				
				final WrappedHttpClient wrappedClient = new WrappedHttpClient(client, proxy);
				clients.add(wrappedClient);
			}
		}
	}
}
