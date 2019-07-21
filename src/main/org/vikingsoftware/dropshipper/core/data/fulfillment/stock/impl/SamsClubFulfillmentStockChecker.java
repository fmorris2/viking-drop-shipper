package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.utils.SamsClubMetaDataParser;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubFulfillmentStockChecker extends AbstractFulfillmentStockChecker<SamsClubWebDriver> {

	private static final SamsClubMetaDataParser metaData = new SamsClubMetaDataParser();
	
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
		metaData.parse(driver.getPageSource());
		
		//TODO ENSURE TITLE MATCHES EXPECTED FULFILLMENT LISTING TITLE
		if(!metaData.passesAllListingConditions()) {
			System.out.println("Sams Club listing does not pass all listing conditions. Setting stock to 0.");
			return 0;
		}
		
		return metaData.getStock();
	}

	@Override
	protected Class<? extends DriverSupplier<?>> getDriverSupplierClass() {
		return SamsClubDriverSupplier.class;
	}

	@Override
	protected double parseItemPrice(SamsClubWebDriver driver) {
		metaData.parse(driver.getPageSource());
		return metaData.getPrice();
	}
}
