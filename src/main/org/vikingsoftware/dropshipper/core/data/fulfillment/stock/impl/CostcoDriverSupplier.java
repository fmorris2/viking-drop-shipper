package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;

public class CostcoDriverSupplier extends DriverSupplier<CostcoWebDriver> {

	private CostcoWebDriver driver = null;

	@Override
	public CostcoWebDriver internalGet() {
		if(driver == null) {
			driver = new CostcoWebDriver();
		}

		return driver;
	}
}
