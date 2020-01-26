package main.org.vikingsoftware.dropshipper.core.net.proxy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class VSDSProxy {
	
	public final String host;
	public final int httpPort, httpsPort, socksPort;
	public final ProxyAuthenticationAccount authenticationAccount;
	
	private VSDSProxy(final Builder builder) {
		this.host = builder.host;
		this.httpPort = builder.httpPort;
		this.httpsPort = builder.httpsPort;
		this.socksPort = builder.socksPort;
		this.authenticationAccount = builder.authenticationAccount;
	}
	
	public boolean supportsSocks() {
		return socksPort != -1;
	}
	
	public boolean supportsHttps() {
		return httpsPort != -1;
	}
	
	public boolean supportsHttp() {
		return httpPort != -1;
	}
	
	public SocketAddress generateSocksAddress() {
		return new InetSocketAddress(host, socksPort);
	}
	
	@Override
	public String toString() {
		return "Host: " + host + "\n" +
				"HTTP Port: " + httpPort + "\n" +
				"HTTPS Port: " + httpsPort + "\n" +
				"SOCKS Port: " + socksPort + "\n" +
				"Authentication Account: " + authenticationAccount + "\n";
	}
	
	public static final class Builder {
		private String host;
		private int httpPort, httpsPort, socksPort;
		private ProxyAuthenticationAccount authenticationAccount;
		
		public Builder host(final String host) {
			this.host = host.trim();
			return this;
		}
		
		public Builder httpPort(final int httpPort) {
			this.httpPort = httpPort;
			return this;
		}
		
		public Builder httpsPort(final int httpsPort) {
			this.httpsPort = httpsPort;
			return this;
		}
		
		public Builder socksPort(final int socksPort) {
			this.socksPort = socksPort;
			return this;
		}
		
		public Builder authenticationAccount(final ProxyAuthenticationAccount account) {
			this.authenticationAccount = account;
			return this;
		}
		
		public VSDSProxy build() {
			return new VSDSProxy(this);
		}
	}
}
