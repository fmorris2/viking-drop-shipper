package main.org.vikingsoftware.dropshipper.order.executor.strategy;

import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;

public interface OrderExecutionStrategy {
	
	public List<Boolean> order(final List<CustomerOrder> order);
}
