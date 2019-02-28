package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AliExpressFulfillmentStockChecker implements FulfillmentStockChecker {
	
	private static final int SELENIUM_INSTANCES_PER_CORE = 3;
	
	private static AliExpressFulfillmentStockChecker instance;
	
	private final ExecutorService threadPool;
	private final BlockingQueue<AliExpressDriverSupplier> webDrivers;
	
	private AliExpressFulfillmentStockChecker() {
		final int coresAvailable = Runtime.getRuntime().availableProcessors();
		final int numThreads = coresAvailable * SELENIUM_INSTANCES_PER_CORE;
		System.out.println("Using " + numThreads + " threads for AliExpressFulfillmentStockChecker");
		threadPool = Executors.newFixedThreadPool(numThreads);
		
		webDrivers = new ArrayBlockingQueue<>(numThreads);
		for(int i = 0; i < numThreads; i++) {
			try {
				webDrivers.put(new AliExpressDriverSupplier());
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static AliExpressFulfillmentStockChecker get() {
		if(instance == null) {
			instance = new AliExpressFulfillmentStockChecker();
		}
		
		return instance;
	}
	
	public static void reset() {
		if(instance != null) {
			try {
				instance.threadPool.shutdownNow();
				instance.webDrivers.forEach(driver -> driver.close());
				instance.webDrivers.clear();
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
		instance = null;
	}
	
	@Override
	public Future<Collection<SkuInventoryEntry>> getStock(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		return threadPool.submit(() -> getStockImpl(marketListing, fulfillmentListing));
	}
	
	private Collection<SkuInventoryEntry> getStockImpl(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		
		final Collection<SkuInventoryEntry> entries = new ArrayList<>();
		AliExpressWebDriver driver = null;
		try {
			driver = webDrivers.take().get();
			if(driver.getReady()) {
				parseAndAddSkuInventoryEntries(driver, marketListing, fulfillmentListing, entries);
				return entries;
			}
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			if(driver != null) {
				driver.close();
			}
		}
		
		return Collections.emptyList();
	}
	
	private void parseAndAddSkuInventoryEntries(final AliExpressWebDriver driver, final MarketplaceListing marketListing,
			final FulfillmentListing fulfillmentListing, final Collection<SkuInventoryEntry> entries) {
		driver.get(fulfillmentListing.listing_url);
		
		final List<SkuMapping> mappings = SkuMappingManager.getMappingsForMarketplaceListing(marketListing.id);
		System.out.println("SKU mappings for marketplace listing " + marketListing.id + ": " + mappings.size());
		if(!mappings.isEmpty()) {
			for(final SkuMapping mapping : mappings) {
				if(driver.selectOrderOptions(mapping, fulfillmentListing)) {
					entries.add(new SkuInventoryEntry(mapping.item_sku, parseItemStock(driver)));
				}
			}
		}
	}
	
	private int parseItemStock(final WebDriver driver) {
		try {
			final WebElement stockNumEl = driver.findElement(By.id("j-sell-stock-num"));
			final String unparsedText = stockNumEl.getText();
			final String parsedText = unparsedText.replaceAll("\\D", "");
			System.out.println("parsed stock: " + parsedText);
			return Integer.parseInt(parsedText);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}

}
