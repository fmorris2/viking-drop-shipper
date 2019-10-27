package main.org.vikingsoftware.dropshipper.order.executor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
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

public class OrderExecutor implements CycleParticipant {

	public static boolean isTestMode = false;

	private static final Map<Integer, ProcessedOrder> successfullyFulfilledOrders = new HashMap<>();

	public static boolean hasBeenFulfilled(final int customerOrderId) {
		return successfullyFulfilledOrders.containsKey(customerOrderId);
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
					
					if(FulfillmentManager.isFrozen(listing.fulfillment_platform_id)) {
						System.out.println("Fulfillment manager " + manager + " is frozen.");
						continue;
					}
					
					if(!FulfillmentManager.get().shouldFulfill(order, listing)) {
						System.out.println("We are waiting on fulfilling customer order " + order.id + " w/ platform " + listing.fulfillment_platform_id);
						continue;
					}
					
					final ProcessedOrder processedOrder = manager.fulfill(order, listing);
	
					if(processedOrder.fulfillment_transaction_id == null) { //TODO LOG UNSUCCESSFUL ORDER
						System.out.println("Failed to fulfill order.");
						failedOrders.add(processedOrder);
					} else {
						System.out.println("Successful order from " + FulfillmentPlatforms.getById(listing.fulfillment_platform_id) + "!");
						if(insertSuccessfulOrderIntoDB(processedOrder)) {
							if(!TransactionUtils.insertTransactionForProcessedOrder(processedOrder)) {
								DBLogging.critical(OrderExecutor.class, "Failed to insert transaction for successful processed order " + processedOrder.id, null);
							}
						}
						break;
					}
				}
			}
	
			//save to DB
			System.out.println("Saving successful orders to DB");
			for(final ProcessedOrder order : successfullyFulfilledOrders.values()) {
				System.out.println("Retry for insertion of processed order...");
				insertSuccessfulOrderIntoDB(order);
			}
	
			System.out.println("Saving failed orders to DB");
			insertFailedOrdersIntoDB(failedOrders);
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

		try (final PreparedStatement prepared = VSDSDBManager.get().createPreparedStatement(sql);
			 final Statement deleteSt = VSDSDBManager.get().createStatement()) {
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
			prepared.setLong(11, System.currentTimeMillis());
			prepared.execute();

			final String removeSql = "DELETE FROM failed_fulfillment_attempts WHERE customer_order_id="+order.customer_order_id;
			deleteSt.execute(removeSql);
			
			successfullyFulfilledOrders.remove(order.customer_order_id);
			System.out.println("Successfully inserted processed order into DB");
			return true;
		} catch (final Exception e) {
			DBLogging.high(getClass(), "failed to insert successful order into DB: ", e);
			successfullyFulfilledOrders.put(order.customer_order_id, order);
		}
		
		return false;
	}

	private void insertFailedOrdersIntoDB(final Collection<ProcessedOrder> failedOrders) {
		//store all new orders in DB
		final String sql = "INSERT INTO failed_fulfillment_attempts(customer_order_id, fulfillment_listing_id) VALUES(?,?)";

		final PreparedStatement prepared = VSDSDBManager.get().createPreparedStatement(sql);
		try {
			for(final ProcessedOrder order : failedOrders) {
				prepared.setInt(1, order.customer_order_id);
				prepared.setInt(2, order.fulfillment_listing_id);
				prepared.addBatch();
			}

			final int numRows = prepared.executeBatch().length;
			System.out.println("Executed batch of " + numRows + " insert queries.");
		} catch (final SQLException e) {
			DBLogging.high(getClass(), "failed to insert failed orders into db: " , e);
		}
	}

}
