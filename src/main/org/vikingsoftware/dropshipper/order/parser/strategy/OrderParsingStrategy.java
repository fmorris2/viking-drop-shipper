package main.org.vikingsoftware.dropshipper.order.parser.strategy;

import java.util.Collection;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;

public interface OrderParsingStrategy {
	
	/**
	 * Connect to the marketplace and parse out all of the new, unknown orders.
	 * 
	 * @return the collection of new, unknown orders. Will never be null.
	 */
	public Collection<CustomerOrder> parseNewOrders();
}
