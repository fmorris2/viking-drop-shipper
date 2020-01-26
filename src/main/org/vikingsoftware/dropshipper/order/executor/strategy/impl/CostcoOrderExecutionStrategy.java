package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class CostcoOrderExecutionStrategy implements OrderExecutionStrategy {

	@Override
	public Optional<ProcessedOrder> order(CustomerOrder order, FulfillmentAccount account,
			FulfillmentListing fulfillmentListing) {
		return Optional.empty();
	}

}
