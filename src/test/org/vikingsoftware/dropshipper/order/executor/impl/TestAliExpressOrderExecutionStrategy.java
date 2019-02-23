package test.org.vikingsoftware.dropshipper.order.executor.impl;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.AliExpressOrderExecutionStrategy;

import org.junit.Assert;
import org.junit.Test;

public class TestAliExpressOrderExecutionStrategy {

	@Test
	public void test() {
		final CustomerOrder mockCustomerOrder = null;
		Assert.assertTrue(new AliExpressOrderExecutionStrategy().testOrder(mockCustomerOrder));
	}

}
