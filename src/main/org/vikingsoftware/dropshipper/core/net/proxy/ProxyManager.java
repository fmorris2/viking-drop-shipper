package main.org.vikingsoftware.dropshipper.core.net.proxy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public final class ProxyManager {
	
	private static final String PROXY_QUERY_STRING = "SELECT hostname,http_port,https_port,socks_port,username,password"
			+ " FROM proxy,proxy_account";
	
	private static ProxyManager instance;
	
	//maps host --> proxy object
	private final Map<String, VSDSProxy> proxies = new HashMap<>();
	
	private ProxyManager() {
		//singleton
	}
	
	public static synchronized ProxyManager get() {
		if(instance == null) {
			instance = new ProxyManager();
			instance.load();
		}
		
		return instance;
	}
	
	public Collection<VSDSProxy> getAllProxies() {
		return proxies.values();
	}
	
	public Optional<VSDSProxy> getProxy(final String host) {
		return Optional.ofNullable(proxies.get(host));
	}
	
	private void load() {
		proxies.clear();
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery(PROXY_QUERY_STRING)) {
			
			while(res.next()) {
				final ProxyAuthenticationAccount account 
					= ProxyAuthenticationAccount.getProxy(res.getString("username"), res.getString("password"));
				
				final VSDSProxySource source = generateProxySourceFromHostName(res.getString("hostname"));
				final VSDSProxy proxy = new VSDSProxy.Builder()
						.host(res.getString("hostname"))
						.httpPort(res.getInt("http_port"))
						.httpsPort(res.getInt("https_port"))
						.socksPort(res.getInt("socks_port"))
						.authenticationAccount(account)
						.source(source)
						.build();
				
				proxies.put(proxy.host, proxy);
			}
			
			System.out.println("[ProxyManager] Successfully loaded " + proxies.size() + " proxies.");
			
		} catch(final SQLException e) {
			e.printStackTrace();
		}
	}
	
	private VSDSProxySource generateProxySourceFromHostName(final String hostname) {
		final String host = hostname.toLowerCase();
		if(host.contains("nordvpn")) {
			return VSDSProxySource.NORD;
		}
		
		return VSDSProxySource.UNKNOWN;
	}
}
