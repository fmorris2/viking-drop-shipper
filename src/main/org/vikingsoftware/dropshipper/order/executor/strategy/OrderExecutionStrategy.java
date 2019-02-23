package main.org.vikingsoftware.dropshipper.order.executor.strategy;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;

public interface OrderExecutionStrategy {
	
	public boolean order(final CustomerOrder order);
}
