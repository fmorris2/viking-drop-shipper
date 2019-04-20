package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;

public class AliExpressFulfillmentStockChecker extends AbstractFulfillmentStockChecker<AliExpressWebDriver> {

	private static AliExpressFulfillmentStockChecker instance;

	private AliExpressFulfillmentStockChecker() {
		super();
	}

	public synchronized static AliExpressFulfillmentStockChecker get() {
		if(instance == null) {
			instance = new AliExpressFulfillmentStockChecker();
		}

		return instance;
	}

	@Override
	protected Class<?> getDriverSupplierClass() {
		return AliExpressDriverSupplier.class;
	}

	@Override
	protected int parseItemStock(final AliExpressWebDriver driver) {
		try {
			final WebElement stockNumEl = driver.findElement(By.id("j-sell-stock-num"));
			final String unparsedText = stockNumEl.getText();
			final String parsedText = unparsedText.replaceAll("\\D", "");
			System.out.println("parsed stock: " + parsedText);
			return Integer.parseInt(parsedText);
		} catch(final Exception e) {
			DBLogging.medium(getClass(), "failed to parse item stock: ", e);
		}

		return 0;
	}
}
