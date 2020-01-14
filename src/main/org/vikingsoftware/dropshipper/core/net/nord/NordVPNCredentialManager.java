package main.org.vikingsoftware.dropshipper.core.net.nord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;

public class NordVPNCredentialManager {
	
	private static final String CREDS_FILE = "/data/nordvpn-creds.secure";
	
	private static String username;
	private static String password;

	static {
		parseCreds();
	}
	
	private NordVPNCredentialManager() {
		
	}
	
	public static String getUsername() {
		return username;
	}
	
	public static String getPassword() {
		return password;
	}
	
	private static void parseCreds() {
		try(
			final InputStream inputStream = EbayApiContextManager.class.getResourceAsStream(CREDS_FILE);
			final InputStreamReader reader = new InputStreamReader(inputStream);
			final BufferedReader bR = new BufferedReader(reader);
		) {
			username = bR.readLine().trim();
			password = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
