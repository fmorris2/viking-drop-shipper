package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.ThreadUtils;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubFulfillmentStockChecker implements FulfillmentStockChecker {

	private static SamsClubFulfillmentStockChecker instance;

	private FulfillmentAccount account;

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
	public Future<Collection<SkuInventoryEntry>> getStock(final FulfillmentAccount account,
			final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		return ThreadUtils.threadPool.submit(() -> {
			System.out.println("Submitting getStockImpl task for market listing " + marketListing.id);
			this.account = account;
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
			if(driver.getReady(account)) {
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
			driver.clearSession(); //failed for whatever reason, we need to make sure we refresh the session amongst browsers
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
		final String pageSource = driver.getPageSource();
		final Pattern pattern = Pattern.compile("availableToSellQuantity\":(\\d+),");
		final Matcher matcher = pattern.matcher(pageSource);
		if(matcher.find()) {
			stock = Integer.parseInt(matcher.group(1));
			System.out.println("Parsed stock: " + stock);
		}
		return stock;
	}
}
