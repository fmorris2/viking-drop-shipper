package main.org.vikingsoftware.dropshipper.core.net;

import org.openqa.selenium.Proxy;

public class VSDSProxy {
	
	public final String host;
	public final int port;
	public final String user;
	public final String pass;
	
	public VSDSProxy(final String host, final int port) {
		this(host, port, null, null);
	}
	
	public VSDSProxy(final String host, final int port, final String user, final String pass) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.pass = pass;
	}
	
	public boolean supportsSocks() {
		return false;
	}
	
	@Override
	public String toString() {
		return host + ":" + port + " user: " + user + ", pass: " + pass;
	}

	public Proxy convertToSeleniumProxy() {
		final Proxy proxy = new Proxy();
		System.out.println("Converting VSDSProxy " + this + " to selenium proxy");
		if(supportsSocks()) {
			proxy.setSocksProxy(host + ":8080");
			proxy.setSocksUsername(user);
			proxy.setSocksPassword(pass);
		} else {
			proxy.setHttpProxy(host + ":" + port);
		}
		
		return proxy;
	}
}
