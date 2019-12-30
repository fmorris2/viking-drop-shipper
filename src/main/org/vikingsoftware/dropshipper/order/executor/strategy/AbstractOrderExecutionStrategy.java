package main.org.vikingsoftware.dropshipper.order.executor.strategy;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public abstract class AbstractOrderExecutionStrategy<T extends LoginWebDriver> implements OrderExecutionStrategy {

	protected static final int RESTART_THRESHOLD = 5;
	protected static final long LOOP_THRESHOLD = 60_000; //60 seconds

	protected ProcessedOrder processedOrder;
	protected DriverSupplier<T> driverSupplier;
	protected T driver;
	protected FulfillmentAccount account;
	protected int timesRestarted;

	protected abstract Class<? extends DriverSupplier<T>> getDriverSupplierClass();
	protected abstract ProcessedOrder executeOrderImpl(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception;

	private long startLoopTime;

	@Override
	public boolean prepareForExecution() {
		System.out.println(this + "#prepareForExecution");
		driverSupplier = BrowserRepository.get().request(getDriverSupplierClass());
		return true;
	}

	@Override
	public ProcessedOrder order(final CustomerOrder order, final FulfillmentAccount account, final FulfillmentListing fulfillmentListing) {
		this.account = account;
		processedOrder = new ProcessedOrder.Builder()
				.customer_order_id(order.id)
				.fulfillment_listing_id(fulfillmentListing.id)
				.build();
			try {
				return executeOrder(order, fulfillmentListing);
			} catch(final Exception e) {
				e.printStackTrace();
				DBLogging.high(getClass(), "order failed: ", e);
			}

		return processedOrder;
	}

	protected ProcessedOrder executeOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception {
		System.out.println(this + "#executeOrder");
		driver = driverSupplier.get();
		if(driver.getReady(account)) {
			System.out.println("\tSuccessfully prepared " + driver);
			return executeOrderImpl(order, fulfillmentListing);
		} else if(timesRestarted < RESTART_THRESHOLD) {
			System.out.println("\tFailed to prepare " + driver + "! Attempting to restart.");
			timesRestarted++;
			return restart(order, fulfillmentListing);
		} else {
			return processedOrder;
		}
	}

	@Override
	public void finishExecution() {
		if(driverSupplier != null) {
			BrowserRepository.get().relinquish(driverSupplier);
			driverSupplier = null;
		} else {
			BrowserRepository.get().replace(getDriverSupplierClass());
		}
		
		driver = null;
	}

	protected ProcessedOrder restart(final CustomerOrder order, final FulfillmentListing listing) throws Exception {
		BrowserRepository.get().replace(this.getDriverSupplierClass());
		driver.close();
		driverSupplier = BrowserRepository.get().request(getDriverSupplierClass());
		return executeOrder(order, listing);
	}

	protected void startLoop() {
		this.startLoopTime = System.currentTimeMillis();
	}

	protected boolean hasExceededThreshold() {
		return System.currentTimeMillis() - startLoopTime >= LOOP_THRESHOLD;
	}

}
