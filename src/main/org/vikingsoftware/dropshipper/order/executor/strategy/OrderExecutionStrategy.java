package main.org.vikingsoftware.dropshipper.order.executor.strategy;

import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;

public interface OrderExecutionStrategy {
	
	public List<ProcessedOrder> order(final List<CustomerOrder> order);
}
