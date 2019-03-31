package main.org.vikingsoftware.dropshipper.core.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public abstract class LoginWebDriver extends ChromeDriver {

	public static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 10;

	protected static final int MAX_LOGIN_TRIES = 20;

	private static final boolean HEADLESS = false;
	private static final ChromeOptions OPTIONS = generateOptions();

	protected String username;
	protected String password;
	protected int loginTries = 0;

	public LoginWebDriver() {
		super(OPTIONS);
		parseCredentials();
	}

	public abstract boolean getReady();
	protected abstract String getCredsFilePath();

	protected static ChromeOptions generateOptions() {
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(HEADLESS);
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		return options;
	}

	private void parseCredentials() {
		try(
				final InputStream inputStream = getClass().getResourceAsStream(getCredsFilePath());
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
