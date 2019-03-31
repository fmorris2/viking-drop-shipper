package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy implements OrderExecutionStrategy {

	@Override
	public boolean prepareForExecution() {
		return false;
	}

	@Override
	public ProcessedOrder order(CustomerOrder order, FulfillmentListing fulfillmentListing) {
		return null;
	}

	@Override
	public void finishExecution() {
	}

}
