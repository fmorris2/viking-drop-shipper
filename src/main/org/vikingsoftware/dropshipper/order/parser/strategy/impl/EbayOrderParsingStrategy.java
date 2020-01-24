package main.org.vikingsoftware.dropshipper.order.parser.strategy.impl;

import java.util.ArrayList;
import java.util.Collection;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;

public class EbayOrderParsingStrategy implements OrderParsingStrategy {

	@Override
	public Collection<CustomerOrder> parseNewOrders() {
		System.out.println("[EbayOrderParsingStrategy] About to call EbayCalls#getOrdersLastXDays");
		final CustomerOrder[] allOrders = EbayCalls.getOrdersLastXDays(2);
		final Collection<CustomerOrder> newOrders = new ArrayList<>();
		for(final CustomerOrder order : allOrders) {
			if(order == null) {
				continue;
			}
			
			if(!Marketplaces.EBAY.getMarketplace().isOrderIdKnown(order.marketplace_order_id)) {
				System.out.println("New eBay order found: " + order.marketplace_order_id);
				newOrders.add(order);
			}
		}

		return newOrders;
	}
}
