package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubDriverSupplier extends DriverSupplier<SamsClubWebDriver> {

	private SamsClubWebDriver driver = null;

	@Override
	public SamsClubWebDriver get() {
		if(driver == null) {
			driver = new SamsClubWebDriver();
		}

		return driver;
	}
}
