package main.org.vikingsoftware.dropshipper.core.shippo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;

public final class ShippoApiContextManager {
	
	private static final String CREDS_FILE = "/data/shippo-api-keys.secure";
	
	private static String liveKey;
	private static String testKey;
	
	static {
		parseCreds();
	}
	
	private ShippoApiContextManager() {
		
	}
	
	public static String getLiveKey() {
		return liveKey;
	}
	
	public static String getTestKey() {
		return testKey;
	}
	
	private static void parseCreds() {
		try(
			final InputStream inputStream = EbayApiContextManager.class.getResourceAsStream(CREDS_FILE);
			final InputStreamReader reader = new InputStreamReader(inputStream);
			final BufferedReader bR = new BufferedReader(reader);
		) {
			liveKey = bR.readLine().trim();
			testKey = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
