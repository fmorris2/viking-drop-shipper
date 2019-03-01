package main.org.vikingsoftware.dropshipper.core.web;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public abstract class LoginWebDriver extends ChromeDriver {
	
	private static final boolean HEADLESS = false;
	private static final ChromeOptions OPTIONS = generateOptions();
	
	public LoginWebDriver() {
		super(OPTIONS);
	}
	
	public abstract boolean getReady();
	
	protected static ChromeOptions generateOptions() {
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(HEADLESS);
		return options;
	}
}
