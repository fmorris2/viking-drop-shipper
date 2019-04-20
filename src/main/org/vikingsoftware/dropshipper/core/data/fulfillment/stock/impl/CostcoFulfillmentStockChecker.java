package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;

public class CostcoFulfillmentStockChecker extends AbstractFulfillmentStockChecker<CostcoWebDriver> {

	private static CostcoFulfillmentStockChecker instance;

	private CostcoFulfillmentStockChecker() {
		super();
	}

	public synchronized static CostcoFulfillmentStockChecker get() {
		if(instance == null) {
			instance = new CostcoFulfillmentStockChecker();
		}

		return instance;
	}

	@Override
	protected int parseItemStock(CostcoWebDriver driver) {
		int stock = 0;
		final String pageSource = driver.getPageSource();
		final Pattern pattern = Pattern.compile("\"ordinal\" : \"(.+)\",)");
		final Matcher matcher = pattern.matcher(pageSource);
		if(matcher.find()) {
			stock = (int)Double.parseDouble(matcher.group(1));
			System.out.println("Parsed stock: " + stock);
		}
		return stock;
	}

	@Override
	protected Class<?> getDriverSupplierClass() {
		return CostcoDriverSupplier.class;
	}
}
