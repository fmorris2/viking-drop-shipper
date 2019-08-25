package test.org.vikingsoftware.dropshipper.pricing.margins;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.pricing.margins.MarginAdjuster;

public class TestMarginAdjuster {
	
	@Test
	public void test() {
		final MarginAdjuster marginAdjuster = new MarginAdjuster();
		//for(int i = 0; i < 1000; i++) {
			marginAdjuster.cycle();
		//}
		Assert.assertTrue(true);
	}
}
