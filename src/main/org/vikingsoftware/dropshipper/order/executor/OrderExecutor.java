package main.org.vikingsoftware.dropshipper.order.executor;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.TransactionUtils;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;

public class OrderExecutor implements CycleParticipant {

	public static boolean isTestMode = false;

	private static final Map<Integer, ProcessedOrder> successfullyFulfilledOrders = new HashMap<>();

	public static boolean hasBeenFulfilled(final int customerOrderId) {
		return successfullyFulfilledOrders.containsKey(customerOrderId);
	}
	
	public static void main(final String[] args) {
		new OrderExecutor().cycle();
	}

	@Override
	public void cycle() {
		final List<CustomerOrder> ordersToExecute = CustomerOrderManager.loadOrdersToExecute();

		ordersToExecute.removeIf(order -> hasBeenFulfilled(order.id));

		if(ordersToExecute.isEmpty()) {
			return;
		}

		final List<ProcessedOrder> failedOrders = new ArrayList<>();
		final FulfillmentManager manager = FulfillmentManager.get();
		try {
			manager.prepareForFulfillment();
			
			for(final CustomerOrder order : ordersToExecute) {
				final List<FulfillmentListing> fulfillmentListings = manager.getListingsForOrder(order);
				for(final FulfillmentListing listing : fulfillmentListings) {
					
					if(!FulfillmentManager.canExecuteOrders(listing.fulfillment_platform_id)) {
						System.out.println("Fulfillment manager " + manager + " is frozen.");
						continue;
					}
					
					if(!FulfillmentManager.get().shouldFulfill(order, listing)) {
						System.out.println("We are waiting on fulfilling customer order " + order.id + " w/ platform " + listing.fulfillment_platform_id);
						continue;
					}
					
					try {
						final ProcessedOrder processedOrder = manager.fulfill(order, listing);						
		
						if(processedOrder.fulfillment_transaction_id == null) { //TODO LOG UNSUCCESSFUL ORDER
							System.out.println("Failed to fulfill order.");
							failedOrders.add(processedOrder);
						} else {
							System.out.println("Successful order from " + FulfillmentPlatforms.getById(listing.fulfillment_platform_id) + "!");
							if(insertSuccessfulOrderIntoDB(processedOrder)) {
								TransactionUtils.insertTransactionForProcessedOrder(order, processedOrder);
							}
							break;
						}
					} catch (final OrderExecutionException e) {
						e.printStackTrace();
						DBLogging.high(getClass(), "Failed to fulfill processed order", e);
					}
				}
			}
	
			//save to DB
			System.out.println("Saving successful orders to DB");
			for(final ProcessedOrder order : successfullyFulfilledOrders.values()) {
				System.out.println("Retry for insertion of processed order...");
				insertSuccessfulOrderIntoDB(order);
			}
	
		} finally {
			manager.endFulfillment();
		}
	}

	private boolean insertSuccessfulOrderIntoDB(final ProcessedOrder order) {
		//store all new orders in DB
		final String sql = "INSERT INTO processed_order(customer_order_id, fulfillment_listing_id, "
				+ "fulfillment_account_id, fulfillment_transaction_id,"
				+ "buy_subtotal, buy_sales_tax, buy_shipping, buy_product_fees, buy_total, profit, date_processed) "
				+ "VALUES(?,?,?,?,?,?,?,?,?,?,?)";

		try (final PreparedStatement prepared = VSDSDBManager.get().createPreparedStatement(sql)) {
			prepared.setInt(1, order.customer_order_id);
			prepared.setInt(2, order.fulfillment_listing_id);
			prepared.setInt(3, order.fulfillment_account_id);
			prepared.setString(4, order.fulfillment_transaction_id);
			prepared.setDouble(5, order.buy_subtotal);
			prepared.setDouble(6, order.buy_sales_tax);
			prepared.setDouble(7, order.buy_shipping);
			prepared.setDouble(8, order.buy_product_fees);
			prepared.setDouble(9, order.buy_total);
			prepared.setDouble(10, order.profit);
			prepared.setLong(11, order.date_processed);
			prepared.execute();
			
			successfullyFulfilledOrders.remove(order.customer_order_id);
			System.out.println("Successfully inserted processed order into DB");
			return true;
		} catch (final Exception e) {
			DBLogging.high(getClass(), "failed to insert successful order into DB: ", e);
			successfullyFulfilledOrders.put(order.customer_order_id, order);
		}
		
		return false;
	}

}
