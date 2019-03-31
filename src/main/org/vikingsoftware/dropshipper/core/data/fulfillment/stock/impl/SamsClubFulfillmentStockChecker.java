package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriverQueue;

public class SamsClubFulfillmentStockChecker extends SamsClubWebDriverQueue implements FulfillmentStockChecker {

	private static final int IN_STOCK_ASSUMPTION = 10;
	private static final int LOW_STOCK_ASSUMPTION = 1;

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
	public Future<Collection<SkuInventoryEntry>> getStock(MarketplaceListing marketListing, FulfillmentListing fulfillmentListing) {
		return threadPool.submit(() -> getStockImpl(marketListing, fulfillmentListing));
	}

	private Collection<SkuInventoryEntry> getStockImpl(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {

		final Collection<SkuInventoryEntry> entries = new ArrayList<>();
		SamsClubDriverSupplier supplier = null;
		SamsClubWebDriver driver = null;
		try {
			supplier = webDrivers.take();
			driver = supplier.get();
			if(driver.getReady()) {
				parseAndAddSkuInventoryEntries(driver, marketListing, fulfillmentListing, entries);
				return entries;
			}
		} catch(final Exception e) {
			DBLogging.medium(getClass(), "failed to get stock for market listing " + marketListing + " and fulfillment listing "
					+ fulfillmentListing + ": ", e);
			SamsClubWebDriver.clearSession(); //failed for whatever reason, we need to make sure we refresh the session amongst browsers
		} finally {
			if(supplier != null) {
				webDrivers.addFirst(supplier);
			}
		}

		return Collections.emptyList();
	}

	private void parseAndAddSkuInventoryEntries(final SamsClubWebDriver driver, final MarketplaceListing marketListing,
			final FulfillmentListing fulfillmentListing, final Collection<SkuInventoryEntry> entries) {
		driver.get(fulfillmentListing.listing_url);

		final List<SkuMapping> mappings = SkuMappingManager.getMappingsForMarketplaceListing(marketListing.id);
		System.out.println("SKU mappings for marketplace listing " + marketListing.id + ": " + mappings.size());
		if(!mappings.isEmpty()) {

		} else {
			entries.add(new SkuInventoryEntry(null, parseItemStock(driver)));
		}
	}

	private int parseItemStock(final WebDriver driver) {
		int stock = 0;
		try {
			final Optional<WebElement> submitButton = driver.findElements(By.className("sc-btn"))
					.stream()
					.filter(el -> el.getText().toLowerCase().contains("ship this item"))
					.findFirst();

			if(submitButton.isPresent()) {
				try {
					final WebElement stockStatus = driver.findElement(By.className("sc-channel-stock-status"));
					switch(stockStatus.getText().toLowerCase()) {
						case "low in stock":
							stock = LOW_STOCK_ASSUMPTION;
						break;
					}
				} catch(final NoSuchElementException e) {
					stock = IN_STOCK_ASSUMPTION;
				}
			}

		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.medium(getClass(), "failed to parse item stock: ", e);
		}

		System.out.println("Stock: " + stock);
		return stock;
	}
}
