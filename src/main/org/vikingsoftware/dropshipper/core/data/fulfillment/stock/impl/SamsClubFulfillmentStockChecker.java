package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubFulfillmentStockChecker extends AbstractFulfillmentStockChecker<SamsClubWebDriver> {

	private static SamsClubFulfillmentStockChecker instance;

	private SamsClubFulfillmentStockChecker() {
		super();
	}

	public synchronized static SamsClubFulfillmentStockChecker get() {
		if(instance == null) {
			instance = new SamsClubFulfillmentStockChecker();
		}

		return instance;
	}

	@Override
	protected int parseItemStock(final SamsClubWebDriver driver) {
		System.out.println("SamsClubFulfillmentStockChecker#parseItemStock");
		int stock = 0;
		final String pageSource = driver.getPageSource();
		final Pattern pattern = Pattern.compile("availableToSellQuantity\":(\\d+),");
		final Matcher matcher = pattern.matcher(pageSource);
		if(matcher.find()) {
			stock = Integer.parseInt(matcher.group(1));
			System.out.println("Parsed stock: " + stock);
		}
		return stock;
	}

	@Override
	protected Class<? extends DriverSupplier<?>> getDriverSupplierClass() {
		return SamsClubDriverSupplier.class;
	}
}
