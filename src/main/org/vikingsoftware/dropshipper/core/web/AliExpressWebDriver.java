package main.org.vikingsoftware.dropshipper.core.web;

import org.openqa.selenium.chrome.ChromeDriver;

public class AliExpressWebDriver extends ChromeDriver implements LoginWebDriver {

	@Override
	public boolean getReady() {
		return false;
	}
	
}
