package main.org.vikingsoftware.dropshipper.order.parser.strategy;

import java.util.Collection;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;

public interface OrderParsingStrategy {
	
	/**
	 * Given a list of already known and parsed order ids, we will
	 * connect to our marketplace and parse out all of the new, unknown orders.
	 * 
	 * @param knownOrderIds the set of previously known and parsed order ids
	 * @return the collection of new, unknown orders. Will never be null.
	 */
	public Collection<CustomerOrder> parseNewOrders(final Set<String> knownOrderIds);
}
