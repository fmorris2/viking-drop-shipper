package main.org.vikingsoftware.dropshipper.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.DeathByCaptcha.Captcha;
import com.DeathByCaptcha.Client;
import com.DeathByCaptcha.HttpClient;

public class CaptchaUtils {
	
	private static final String CREDS_FILE_PATH = "/data/death-by-captcha-creds.secure";
	
	private static String username;
	private static String password;
	
	static {
		parseCreds();
	}
	
	public static String solveSimpleCaptcha(final File imgFile) {
		try {
			final Client client = new HttpClient(username, password);
			client.isVerbose = true;
			if(client.getBalance() > 0) {
				final Captcha captcha = client.decode(imgFile);
				if(captcha != null) {
					return captcha.text;
				}
			}
		} catch(final Exception e) {
			DBLogging.high(CaptchaUtils.class, "failed to solve simple captcha: ", e);
		}
		
		return null;
	}
	
	private static void parseCreds() {
		try(
				final InputStream inputStream = CaptchaUtils.class.getResourceAsStream(CREDS_FILE_PATH);
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
