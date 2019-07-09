package test.org.vikingsoftware.dropshipper.order.executor.impl;

import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.CostcoOrderExecutionStrategy;

public class TestCostcoOrderExecutionStrategy {

	private static final int NUM_ITERATIONS = 1000;

	@Test
	public void test() {
		OrderExecutor.isTestMode = true;
		final CustomerOrder customerOrder = CustomerOrderManager.loadCustomerOrderById(123).get();

		final OrderExecutionStrategy strategy = new CostcoOrderExecutionStrategy();
		Assert.assertTrue(strategy.prepareForExecution());

		FulfillmentManager.get().load();
		final FulfillmentListing listing = FulfillmentManager.get().getListingsForOrder(customerOrder).stream()
				.filter(list -> FulfillmentPlatforms.getById(list.fulfillment_platform_id) == FulfillmentPlatforms.COSTCO)
				.findFirst().orElse(null);

		Assert.assertNotNull(listing);

		for(int i = 0; i < NUM_ITERATIONS; i++) {
			final FulfillmentAccount account = FulfillmentAccountManager.get().getAndRotateEnabledAccount(FulfillmentPlatforms.COSTCO);
			Assert.assertTrue(strategy.order(customerOrder, account, listing).fulfillment_transaction_id != null);
		}
		strategy.finishExecution();
	}

}
