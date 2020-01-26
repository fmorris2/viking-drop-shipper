package main.org.vikingsoftware.dropshipper.core.net.proxy;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Optional;

public class ProxyAuthenticator extends Authenticator {
	
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		final String proxyHost = this.getRequestingHost();
		
		PasswordAuthentication passwordAuth = super.getPasswordAuthentication();
		
		if (proxyHost.contains("nordvpn")) {
			final Optional<VSDSProxy> proxy = ProxyManager.get().getProxy(proxyHost);
			if (proxy.isPresent()) {
				final ProxyAuthenticationAccount account = proxy.get().authenticationAccount;
				System.out.println("Providing Authentication for NordVPN proxy: " + proxy);
				final String user = account.username;
				final String pass = account.password;
				passwordAuth = new PasswordAuthentication(user, pass.toCharArray());
			}
		}
		
		return passwordAuth;
	}
	
}
