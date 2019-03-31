package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.function.Supplier;

import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubDriverSupplier implements Supplier<SamsClubWebDriver> {

	private SamsClubWebDriver driver = null;

	@Override
	public SamsClubWebDriver get() {
		if(driver == null) {
			driver = new SamsClubWebDriver();
		}

		return driver;
	}

	public void close() {
		if(driver != null) {
			driver.quit();
		}
	}

}
