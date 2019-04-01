package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy implements OrderExecutionStrategy {

	private ProcessedOrder processedOrder;
	@Override
	public boolean prepareForExecution() {
		return false;
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
				DBLogging.high(getClass(), "order failed: ", e);
			}

		return processedOrder;
	}

	private ProcessedOrder executeOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		return processedOrder;
	}

	@Override
	public void finishExecution() {
	}

}
