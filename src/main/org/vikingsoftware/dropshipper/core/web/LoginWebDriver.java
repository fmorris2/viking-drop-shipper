package main.org.vikingsoftware.dropshipper.core.web;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;

public abstract class LoginWebDriver extends ChromeDriver {

	public static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 10;

	protected static final int MAX_LOGIN_TRIES = 20;

	private static final boolean HEADLESS = false;
	private static final ChromeOptions OPTIONS = generateOptions();

	protected int loginTries = 0;

	public LoginWebDriver() {
		super(OPTIONS);
	}

	public abstract boolean getReady(final FulfillmentAccount account);

	protected static ChromeOptions generateOptions() {
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(HEADLESS);
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		return options;
	}
}
