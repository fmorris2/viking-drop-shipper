package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriver;

public class AliExpressFulfillmentStockChecker implements FulfillmentStockChecker {
	
	private static final int SELENIUM_INSTANCES_PER_CORE = 1;
	
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
		AliExpressWebDriver driver = null;
		try {
			driver = webDrivers.take().get();
			if(driver.getReady()) {
				
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
	
	private static class AliExpressDriverSupplier implements Supplier<AliExpressWebDriver> {

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
				driver.close();
			}
		}
		
	}

}
