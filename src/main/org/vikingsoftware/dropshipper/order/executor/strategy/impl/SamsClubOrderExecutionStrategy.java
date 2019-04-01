package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy implements OrderExecutionStrategy {

	private ProcessedOrder processedOrder;

	private DriverSupplier<SamsClubWebDriver> driverSupplier;
	private SamsClubWebDriver driver;

	@Override
	public boolean prepareForExecution() {
		System.out.println("SamsClubOrderExecutionStrategy#prepareForExecution");
		driverSupplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		return true;
	}

	@Override
	public ProcessedOrder order(CustomerOrder order, FulfillmentListing fulfillmentListing) {
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

	private ProcessedOrder executeOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		System.out.println("SamsClubOrderExecutionStrategy#executeOrder");
		driver = driverSupplier.get();
		if(driver.getReady()) {
			System.out.println("\tSuccessfully prepared sams club driver");
			driver.get(fulfillmentListing.listing_url);
		} else {
			System.out.println("\tFailed to prepare sams club driver! Attempting to restart.");
			return restart(order, fulfillmentListing);
		}
		return processedOrder;
	}

	private ProcessedOrder restart(final CustomerOrder order, final FulfillmentListing listing) {
		driver.quit();
		BrowserRepository.get().replace(driverSupplier);
		driverSupplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		return executeOrder(order, listing);
	}

	@Override
	public void finishExecution() {
		BrowserRepository.get().relinquish(driverSupplier);
		driver = null;
		driverSupplier = null;
	}

}
