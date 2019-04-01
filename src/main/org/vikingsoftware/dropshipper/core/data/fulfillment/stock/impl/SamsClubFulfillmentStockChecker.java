package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.ThreadUtils;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubFulfillmentStockChecker implements FulfillmentStockChecker {

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
		return ThreadUtils.threadPool.submit(() -> {
			System.out.println("Submitting getStockImpl task for market listing " + marketListing.id);
			return getStockImpl(marketListing, fulfillmentListing);
		});
	}

	private Collection<SkuInventoryEntry> getStockImpl(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		System.out.println("getStockImpl for market listing " + marketListing.id);
		final Collection<SkuInventoryEntry> entries = new ArrayList<>();
		SamsClubDriverSupplier supplier = null;
		SamsClubWebDriver driver = null;
		try {
			supplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
			driver = supplier.get();
			if(driver.getReady()) {
				System.out.println("Successfully prepared SamsClubDriver");
				parseAndAddSkuInventoryEntries(driver, marketListing, fulfillmentListing, entries);
				return entries;
			} else {
				System.out.println("failed to get sams club driver ready!");
				driver.quit();
				BrowserRepository.get().replace(supplier);
			}
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.medium(getClass(), "failed to get stock for market listing " + marketListing + " and fulfillment listing "
					+ fulfillmentListing + ": ", e);
			SamsClubWebDriver.clearSession(); //failed for whatever reason, we need to make sure we refresh the session amongst browsers
		} finally {
			if(supplier != null) {
				BrowserRepository.get().relinquish(supplier);
			}
		}

		System.out.println("Returning empty stock for market listing " + marketListing.id);
		return Collections.emptyList();
	}

	private void parseAndAddSkuInventoryEntries(final SamsClubWebDriver driver, final MarketplaceListing marketListing,
			final FulfillmentListing fulfillmentListing, final Collection<SkuInventoryEntry> entries) {
		System.out.println("parseAndAddSkuInventoryEntries for market listing " + marketListing.id);
		driver.get(fulfillmentListing.listing_url);
		System.out.println("Successfully loaded fulfillment listing url");

		final List<SkuMapping> mappings = SkuMappingManager.getMappingsForMarketplaceListing(marketListing.id);
		System.out.println("SKU mappings for marketplace listing " + marketListing.id + ": " + mappings.size());
		if(!mappings.isEmpty()) {
		} else {
			entries.add(new SkuInventoryEntry(null, parseItemStock(driver)));
		}
	}

	private int parseItemStock(final WebDriver driver) {
		System.out.println("SamsClubFulfillmentStockChecker#parseItemStock");
		int stock = 0;
		try {
			final WebElement submitButton = driver.findElement(By.cssSelector("div.sc-cart-qty-button.online > form > button"));

			System.out.println("Found submit button");
			try {
				driver.manage().timeouts().implicitlyWait(250, TimeUnit.MILLISECONDS);
				final WebElement stockStatus = driver.findElement(By.cssSelector("div.sc-channel.sc-channel-online div.sc-channel-stock > div"));
				switch(stockStatus.getText().toLowerCase()) {
					case "low in stock":
						stock = LOW_STOCK_ASSUMPTION;
					break;

					default:
						stock = IN_STOCK_ASSUMPTION;
				}
			} catch(final NoSuchElementException e) {
				stock = IN_STOCK_ASSUMPTION;
			}

			driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);

		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.medium(getClass(), "failed to parse item stock: ", e);
		}

		System.out.println("Stock: " + stock);
		return stock;
	}
}
