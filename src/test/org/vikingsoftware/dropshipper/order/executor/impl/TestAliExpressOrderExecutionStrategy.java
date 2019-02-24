package test.org.vikingsoftware.dropshipper.order.executor.impl;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.AliExpressOrderExecutionStrategy;

import org.junit.Assert;
import org.junit.Test;

public class TestAliExpressOrderExecutionStrategy {
	
	private static final int NUM_ITERATIONS = 1000;
	
	@Test
	public void test() {
		final CustomerOrder customerOrder = CustomerOrderManager.loadFirstCustomerOrder();
		
		final AliExpressOrderExecutionStrategy strategy = new AliExpressOrderExecutionStrategy();
		Assert.assertTrue(strategy.prepareForExecution());
		
		FulfillmentManager.get().load();
		final FulfillmentListing listing = FulfillmentManager.get().getListingsForOrder(customerOrder).stream()
				.filter(list -> FulfillmentPlatforms.getById(list.fulfillment_platform_id) == FulfillmentPlatforms.ALI_EXPRESS)
				.findFirst().orElse(null);
		
		Assert.assertNotNull(listing);
		
		for(int i = 0; i < NUM_ITERATIONS; i++) {
			Assert.assertTrue(strategy.testOrder(customerOrder, listing).fulfillment_transaction_id != null);
		}
		strategy.finishExecution();
	}

}
