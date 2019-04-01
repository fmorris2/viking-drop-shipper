package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.openqa.selenium.By;
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
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;

public class AliExpressFulfillmentStockChecker implements FulfillmentStockChecker {

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
	public Future<Collection<SkuInventoryEntry>> getStock(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		return ThreadUtils.threadPool.submit(() -> getStockImpl(marketListing, fulfillmentListing));
	}

	private Collection<SkuInventoryEntry> getStockImpl(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {

		final Collection<SkuInventoryEntry> entries = new ArrayList<>();
		DriverSupplier<AliExpressWebDriver> supplier = null;
		AliExpressWebDriver driver = null;
		try {
			supplier = BrowserRepository.get().request(AliExpressDriverSupplier.class);
			driver = supplier.get();
			if(driver.getReady()) {
				parseAndAddSkuInventoryEntries(driver, marketListing, fulfillmentListing, entries);
				return entries;
			}
		} catch(final Exception e) {
			DBLogging.medium(getClass(), "failed to get stock for market listing " + marketListing + " and fulfillment listing "
					+ fulfillmentListing + ": ", e);
		} finally {
			BrowserRepository.get().relinquish(supplier);
		}

		return Collections.emptyList();
	}

	private void parseAndAddSkuInventoryEntries(final AliExpressWebDriver driver, final MarketplaceListing marketListing,
			final FulfillmentListing fulfillmentListing, final Collection<SkuInventoryEntry> entries) {
		driver.get(fulfillmentListing.listing_url);

		final List<SkuMapping> mappings = SkuMappingManager.getMappingsForMarketplaceListing(marketListing.id);
		System.out.println("SKU mappings for marketplace listing " + marketListing.id + ": " + mappings.size());
		if(!mappings.isEmpty()) {
			driver.clearCachedSelectedOrderOptions();
			for(final SkuMapping mapping : mappings) {
				System.out.println("selecting order options for SKU Mapping " + mapping);
				if(driver.selectOrderOptions(mapping, fulfillmentListing)) {
					entries.add(new SkuInventoryEntry(mapping.item_sku, parseItemStock(driver)));
				}
			}
		} else {
			entries.add(new SkuInventoryEntry(null, parseItemStock(driver)));
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
			DBLogging.medium(getClass(), "failed to parse item stock: ", e);
		}

		return 0;
	}

}
