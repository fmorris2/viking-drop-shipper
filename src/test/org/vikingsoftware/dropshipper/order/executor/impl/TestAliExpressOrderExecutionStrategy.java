package test.org.vikingsoftware.dropshipper.order.executor.impl;

import java.util.Collections;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.AliExpressOrderExecutionStrategy;

import org.junit.Assert;
import org.junit.Test;

public class TestAliExpressOrderExecutionStrategy {

	@Test
	public void test() {
		final CustomerOrder customerOrder = CustomerOrderManager.loadFirstCustomerOrder();
		
		FulfillmentManager.load();
		Assert.assertTrue(new AliExpressOrderExecutionStrategy()
			.testOrder(Collections.singletonList(customerOrder)).stream().noneMatch(res -> !res));
	}

}
