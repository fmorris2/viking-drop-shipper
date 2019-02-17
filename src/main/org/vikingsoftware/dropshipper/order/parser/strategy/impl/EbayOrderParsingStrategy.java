package main.org.vikingsoftware.dropshipper.order.parser.strategy.impl;

import java.util.Collection;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;

public class EbayOrderParsingStrategy implements OrderParsingStrategy {

	public Collection<CustomerOrder> parseNewOrders(final Set<String> knownOrderIds) {
		return null;
	}

}
