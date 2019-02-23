package main.org.vikingsoftware.dropshipper.core.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class DBCredentialManager {
	private static final String CREDS_FILE = "/data/db_creds.secure";
	
	private static String user;
	private static String pass;
	private static String db;
	
	static {
		parseCreds();
	}
	
	private static void parseCreds() {
		try(
			final InputStream inputStream = DBCredentialManager.class.getResourceAsStream(CREDS_FILE);
			final InputStreamReader reader = new InputStreamReader(inputStream);
			final BufferedReader bR = new BufferedReader(reader);
		) {
			user = bR.readLine().trim();
			pass = bR.readLine().trim();
			db = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getUser() {
		return user;
	}
	
	public static String getPass() {
		return pass;
	}
	
	public static String getDB() {
		return db;
	}
	
}
