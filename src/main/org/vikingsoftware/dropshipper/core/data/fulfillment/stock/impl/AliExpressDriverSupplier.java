package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;

public class AliExpressDriverSupplier extends DriverSupplier<AliExpressWebDriver> {

	private AliExpressWebDriver driver = null;

	@Override
	public AliExpressWebDriver internalGet() {
		if(driver == null) {
			driver = new AliExpressWebDriver();
		}
		return driver;
	}

}