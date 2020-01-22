package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.Cookie;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.SessionSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubDriverSupplier extends DriverSupplier<SamsClubWebDriver> implements SessionSupplier {

	private final Map<FulfillmentAccount, Object> locks = new ConcurrentHashMap<>();
	private final Map<FulfillmentAccount, Map<String, String>> currentActiveSessions = new HashMap<>();
	
	private SamsClubWebDriver driver = null;

	@Override
	public SamsClubWebDriver internalGet(final VSDSProxy proxy) {
		if(driver == null) {
			driver = new SamsClubWebDriver(proxy);
		}

		return driver;
	}

	@Override
	public Map<String, String> getSession(final FulfillmentAccount account, final VSDSProxy proxy) {
		
		synchronized(locks.computeIfAbsent(account, acc -> new Object())) {
			final Map<String, String> currentActiveSession = currentActiveSessions.computeIfAbsent(account, acc -> new HashMap<>());
			
			if(currentActiveSession.isEmpty()) {
				if(driver == null) {
					internalGet(proxy);
				}
				
				if(driver.getReady(account)) {
					for(final Cookie cookie : driver.manage().getCookies()) {
						currentActiveSession.put(cookie.getName(), cookie.getValue());
					}
				}
			}
			
			return currentActiveSession;
		}
	}
	
	@Override
	public void clearSession(final FulfillmentAccount account) {
		synchronized(locks.computeIfAbsent(account, acc -> new Object())) {
			currentActiveSessions.put(account, new HashMap<>());
		}
	}
}
