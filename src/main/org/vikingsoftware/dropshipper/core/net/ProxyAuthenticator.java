package main.org.vikingsoftware.dropshipper.core.net;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import main.org.vikingsoftware.dropshipper.core.net.nord.NordVPNCredentialManager;

public class ProxyAuthenticator extends Authenticator {
	
	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		final String proxy = this.getRequestingHost();
		
		PasswordAuthentication passwordAuth = super.getPasswordAuthentication();
		if(proxy.contains("nordvpn")) {
			System.out.println("Providing Authentication for NordVPN proxy: " + proxy + " to URL " + this.getRequestingURL());
			//Thread.dumpStack();
			final String user = NordVPNCredentialManager.getUsername();
			final String pass = NordVPNCredentialManager.getPassword();
			passwordAuth = new PasswordAuthentication(user, pass.toCharArray());
		}
		
		return passwordAuth;
	}
	
}
