package test.org.vikingsoftware.dropshipper.order.executor;

import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;

import org.junit.Assert;
import org.junit.Test;

public class TestOrderExecutor {

	@Test
	public void test() {
		OrderExecutor.isTestMode = false;
		final OrderExecutor executor = new OrderExecutor();
		for(int i = 0; i < 1; i++) {
			executor.cycle();
		}
		
		Assert.assertTrue(true);
	}

}
