package main.org.vikingsoftware.dropshipper.core.net;

public final class VSDSProxy {
	
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
	
	@Override
	public String toString() {
		return host + ":" + port + " user: " + user + ", pass: " + pass;
	}
}
