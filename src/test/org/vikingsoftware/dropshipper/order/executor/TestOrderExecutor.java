package test.org.vikingsoftware.dropshipper.order.executor;

import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;

import org.junit.Assert;
import org.junit.Test;

public class TestOrderExecutor {

	@Test
	public void test() {
		OrderExecutor.isTestMode = true;
		final OrderExecutor executor = new OrderExecutor();
		executor.cycle();
		
		Assert.assertTrue(true);
	}

}
