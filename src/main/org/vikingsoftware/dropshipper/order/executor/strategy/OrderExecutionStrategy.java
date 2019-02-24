package main.org.vikingsoftware.dropshipper.order.executor.strategy;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;

public interface OrderExecutionStrategy {
	
	public boolean prepareForExecution();
	public ProcessedOrder order(final CustomerOrder order, final FulfillmentListing fulfillmentListing);
	public void finishExecution();
}
