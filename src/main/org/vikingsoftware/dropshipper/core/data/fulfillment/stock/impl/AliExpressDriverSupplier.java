package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.function.Supplier;

import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;

public class AliExpressDriverSupplier implements Supplier<AliExpressWebDriver> {

	private AliExpressWebDriver driver = null;
	
	@Override
	public AliExpressWebDriver get() {
		if(driver == null) {
			driver = new AliExpressWebDriver();
		}
		return driver;
	}
	
	public void close() {
		if(driver != null) {
			driver.quit();
		}
	}
	
}