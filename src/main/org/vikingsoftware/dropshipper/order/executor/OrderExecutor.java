package main.org.vikingsoftware.dropshipper.order.executor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

public class OrderExecutor implements CycleParticipant {
	
	@Override
	public void cycle() {
		final FulfillmentManager manager = FulfillmentManager.get();
		manager.prepareForFulfillment();
		
		final List<ProcessedOrder> successfulOrders = new ArrayList<>();
		final List<ProcessedOrder> failedOrders = new ArrayList<>();
		final List<CustomerOrder> ordersToExecute = CustomerOrderManager.loadOrdersToExecute();
		
		for(final CustomerOrder order : ordersToExecute) {
			final List<FulfillmentListing> fulfillmentListings = manager.getListingsForOrder(order);
			for(final FulfillmentListing listing : fulfillmentListings) {
				final ProcessedOrder processedOrder = manager.fulfill(order, listing);
				
				if(processedOrder.fulfillment_transaction_id == null) { //TODO LOG UNSUCCESSFUL ORDER
					System.out.println("Failed to fulfill order.");
					failedOrders.add(processedOrder);
				} else {
					System.out.println("Successful order from ali express!");
					successfulOrders.add(processedOrder);
					break;
				}
			}
		}
		
		//save to DB
		System.out.println("Saving successful orders to DB");
		insertSuccessfulOrdersIntoDB(successfulOrders);
		
		System.out.println("Saving failed orders to DB");
		insertFailedOrdersIntoDB(failedOrders);
		manager.endFulfillment();
	}
	
	private void insertSuccessfulOrdersIntoDB(final Collection<ProcessedOrder> successfulOrders) {
		//store all new orders in DB
		final String sql = "INSERT INTO processed_orders(customer_order_id, fulfillment_listing_id, fulfillment_transaction_id,"
				+ "order_status) VALUES(?,?,?,?)";
		
		final PreparedStatement prepared = VDSDBManager.get().createPreparedStatement(sql);
		final Statement deleteBatch = VDSDBManager.get().createStatement();
		try {
			for(final ProcessedOrder order : successfulOrders) {
				prepared.setInt(1, order.customer_order_id);
				prepared.setInt(2, order.fulfillment_listing_id);
				prepared.setString(3, order.fulfillment_transaction_id);
				prepared.setString(4, order.order_status);
				prepared.addBatch();
				
				final String removeSql = "DELETE FROM failed_fulfillment_attempts WHERE customer_order_id="+order.customer_order_id;
				deleteBatch.addBatch(removeSql);
			}
		
			deleteBatch.executeBatch();
			final int numRows = prepared.executeBatch().length;
			System.out.println("Executed batch of " + numRows + " insert queries.");
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void insertFailedOrdersIntoDB(final Collection<ProcessedOrder> failedOrders) {
		//store all new orders in DB
		final String sql = "INSERT INTO failed_fulfillment_attempts(customer_order_id, fulfillment_listing_id) VALUES(?,?)";
		
		final PreparedStatement prepared = VDSDBManager.get().createPreparedStatement(sql);
		try {
			for(final ProcessedOrder order : failedOrders) {
				prepared.setInt(1, order.customer_order_id);
				prepared.setInt(2, order.fulfillment_listing_id);
				prepared.addBatch();
			}
		
			final int numRows = prepared.executeBatch().length;
			System.out.println("Executed batch of " + numRows + " insert queries.");
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

}
