package main.org.vikingsoftware.dropshipper.order.executor.strategy;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;

public interface OrderExecutionStrategy {
	public Optional<ProcessedOrder> order(final CustomerOrder order, 
			final FulfillmentAccount account, final FulfillmentListing fulfillmentListing);
}
