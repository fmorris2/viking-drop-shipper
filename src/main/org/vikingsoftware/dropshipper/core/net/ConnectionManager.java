package main.org.vikingsoftware.dropshipper.core.net;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jsoup.Connection;
import org.jsoup.Jsoup;


public final class ConnectionManager {
	
	private static final int NUM_CONNECTIONS_BEFORE_CYCLE = 1000;
	private static final long FLAG_SLEEP_MS = 30_000;
	
	// Format: us4112.nordvpn.com
	private static final String[] US_NORD_PROXY_IDS = {
		"4072", "4110", "4112", "4113", "4122", "4123", "4125",
		"4126", "4128", "4603", "4608"
	};
	
	private static ConnectionManager instance;
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Queue<Connection> connections = new LinkedList<>();
	
	private int numConnections = 0;
	
	private ConnectionManager() {
		//singleton can't be instantiated from the outside...
	}
	
	public static synchronized ConnectionManager get() {
		if(instance == null) {
			instance = new ConnectionManager();
			Authenticator.setDefault(new ProxyAuthenticator());
			instance.populateConnections();
		}
		
		return instance;
	}
	
	public Connection getConnection() {
		lock.writeLock().lock();
		try {
			final Connection connection = connections.peek();
			
			if(++numConnections >= NUM_CONNECTIONS_BEFORE_CYCLE) {
				connections.add(connections.poll());
				numConnections = 0;
			}
			return connection;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void flag() {
		lock.writeLock().lock();
		try {
			System.out.println("Rotating proxy: " + connections.peek());
			connections.add(connections.poll());
			numConnections = 0;
			Thread.sleep(FLAG_SLEEP_MS);
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private void populateConnections() {
		addNordProxies();
		
		connections.add(null);
	}
	
	private void addNordProxies() {
		for(final String id : US_NORD_PROXY_IDS) {
			final String url = "us" + id + ".nordvpn.com";
			System.out.println("Adding NordVPN proxy: " + url + ":80");
			final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(url, 80));
			connections.add(Jsoup.connect("http://www.google.com/").proxy(proxy));
		}
	}
}
