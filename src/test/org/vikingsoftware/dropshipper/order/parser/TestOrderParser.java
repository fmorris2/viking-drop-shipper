package test.org.vikingsoftware.dropshipper.order.parser;

import static org.junit.Assert.fail;
import main.org.vikingsoftware.dropshipper.order.parser.OrderParser;

import org.junit.Assert;
import org.junit.Test;

public class TestOrderParser {

	@Test
	public void test() {
		try {
			new OrderParser().cycle();
			Assert.assertTrue(true);
		} catch(final Exception e) {
			e.printStackTrace();
			fail("Order Parser failed to execute");
		}
	}

}
