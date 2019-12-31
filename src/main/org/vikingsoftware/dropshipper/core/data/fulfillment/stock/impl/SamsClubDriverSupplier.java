package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.Cookie;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.SessionSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubDriverSupplier extends DriverSupplier<SamsClubWebDriver> implements SessionSupplier {

	private final Map<String, String> currentActiveSession = new HashMap<>();
	
	private SamsClubWebDriver driver = null;

	@Override
	public SamsClubWebDriver internalGet() {
		if(driver == null) {
			driver = new SamsClubWebDriver();
		}

		return driver;
	}

	@Override
	public Map<String, String> getSession(final FulfillmentAccount account) {
		
		if(currentActiveSession.isEmpty()) {
			if(driver == null) {
				internalGet();
			}
			
			if(driver.getReady(account)) {
				for(final Cookie cookie : driver.manage().getCookies()) {
					currentActiveSession.put(cookie.getName(), cookie.getValue());
				}
			}
		}
		
		return currentActiveSession;
	}
	
	@Override
	public void clearSession() {
		currentActiveSession.clear();
	}
}
