package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.ThreadUtils;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public abstract class AbstractFulfillmentStockChecker<T extends LoginWebDriver> implements FulfillmentStockChecker {

	protected FulfillmentAccount account;

	protected abstract Class<? extends DriverSupplier<?>> getDriverSupplierClass();
	protected abstract int parseItemStock(final T driver);
	protected abstract double parseItemPrice(final T driver);

	@Override
	public Future<Collection<Pair<Integer,Double>>> getStock(final FulfillmentAccount account,
			final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		return ThreadUtils.threadPool.submit(() -> {
			System.out.println("Submitting getStockImpl task for market listing " + marketListing.id);
			this.account = account;
			return getStockImpl(marketListing, fulfillmentListing);
		});
	}

	protected Collection<Pair<Integer,Double>> getStockImpl(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		System.out.println("getStockImpl for market listing " + marketListing.id);
		final Collection<Pair<Integer,Double>> entries = new ArrayList<>();
		DriverSupplier<? extends T> supplier = null;
		T driver = null;
		try {
			supplier = BrowserRepository.get().request(getDriverSupplierClass());
			driver = supplier.get();
			if(driver.getReady(account)) {
				System.out.println("Successfully prepared " + driver);
				System.out.println("Parsing stock for fulfillment listing: " + fulfillmentListing.listing_url);
				parseAndAddSkuInventoryEntries(driver, marketListing, fulfillmentListing, entries);
				return entries;
			} else {
				supplier = null;
				System.out.println("failed to get " + driver + " ready!");
				driver.close();
				System.out.println("\tsuccessfully quit web driver.");
			}
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.medium(getClass(), "failed to get stock for market listing " + marketListing + " and fulfillment listing "
					+ fulfillmentListing + ": ", e);
			driver.clearSession(); //failed for whatever reason, we need to make sure we refresh the session amongst browsers
		} finally {
			if(supplier != null) {
				BrowserRepository.get().relinquish(supplier);
			} else {
				BrowserRepository.get().replace(getDriverSupplierClass());
				System.out.println("\tsuccessfully replaced driver supplier");
			}
		}

		System.out.println("Returning empty stock for market listing " + marketListing.id);
		return Collections.emptyList();
	}

	protected void parseAndAddSkuInventoryEntries(final T driver, final MarketplaceListing marketListing,
			final FulfillmentListing fulfillmentListing, final Collection<Pair<Integer,Double>> entries) {
		driver.get(fulfillmentListing.listing_url);

		entries.add(new Pair<>(parseItemStock(driver), parseItemPrice(driver)));
	}
}
