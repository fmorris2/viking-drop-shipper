package main.org.vikingsoftware.dropshipper.order.tracking.handler;

import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.order.tracking.error.UnknownTrackingIdException;

public abstract class AbstractOrderTrackingHandler<T extends LoginWebDriver> implements OrderTrackingHandler  {

	protected abstract Class<? extends DriverSupplier<T>> getDriverSupplierClass();
	protected abstract TrackingEntry parseTrackingInfo(final T driver, final ProcessedOrder order);

	@Override
	public RunnableFuture<TrackingEntry> getTrackingInfo(ProcessedOrder order) {
		return new FutureTask<>(() -> getTrackingInfoImpl(order));
	}

	protected TrackingEntry getTrackingInfoImpl(final ProcessedOrder order) {
		System.out.println("Initiating tracking info process for processed order " + order.id);
		T driver = null;
		DriverSupplier<T> supplier = null;
		try {
			supplier = BrowserRepository.get().request(getDriverSupplierClass());
			driver = supplier.get();
			final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(order.fulfillment_account_id);
			if(driver.getReady(account)) {
				return parseTrackingInfo(driver, order);
			} else {
				return restart(order, supplier);
			}
		} catch(final UnknownTrackingIdException e) {
			DBLogging.high(getClass(), "Unknown courier for tracking number!", e);
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			if(supplier != null) {
				BrowserRepository.get().relinquish(supplier);
			}
		}
		return null;
	}

	protected TrackingEntry restart(final ProcessedOrder order, final DriverSupplier<T> supplier) {
		supplier.get().quit();
		BrowserRepository.get().replace(supplier);
		return getTrackingInfoImpl(order);
	}
}
