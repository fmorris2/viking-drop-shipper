package test.org.vikingsoftware.dropshipper.core.data;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;

import org.junit.Assert;
import org.junit.Test;

public class TestFulfillmentManager {

	@Test
	public void test() {
		FulfillmentManager.get().prepareForFulfillment();
		Assert.assertTrue(FulfillmentManager.get().isLoaded());
		FulfillmentManager.get().endFulfillment();
	}

}
