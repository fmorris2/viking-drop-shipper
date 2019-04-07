package test.org.vikingsoftware.dropshipper.order.tracking;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.order.tracking.OrderTracking;

public class TestOrderTracking {

	@Test
	public void test() {
		new OrderTracking().cycle();
		Assert.assertTrue(true);
	}

}
