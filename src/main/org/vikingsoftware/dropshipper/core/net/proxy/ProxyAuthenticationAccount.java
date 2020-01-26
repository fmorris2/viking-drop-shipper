package main.org.vikingsoftware.dropshipper.core.net.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProxyAuthenticationAccount {
	
	private static final Map<Integer, ProxyAuthenticationAccount> ACCOUNT_CACHE = new HashMap<>();
	
	public final String username;
	public final String password;
	
	private ProxyAuthenticationAccount(final String user, final String pass) {
		this.username = user;
		this.password = pass;
	}
	
	public static ProxyAuthenticationAccount getProxy(final String user, final String pass) {
		final int hash = Objects.hash(user, pass);
		return ACCOUNT_CACHE.computeIfAbsent(hash, hashCode -> new ProxyAuthenticationAccount(user, pass));
	}
}
