package main.org.vikingsoftware.dropshipper.order.tracking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class OrderTracking implements CycleParticipant {

	private static final int TASK_STARTER_THREADS = 5;
	private final ExecutorService taskStarter = Executors.newFixedThreadPool(TASK_STARTER_THREADS);

	@Override
	public void cycle() {

		if(!TrackingManager.get().prepareForCycle()) {
			System.out.println("Tracking manager failed to prepare for cycle...");
			return;
		}

		if(!FulfillmentManager.get().isLoaded()) {
			FulfillmentManager.get().load();
		}

		final List<ProcessedOrder> untrackedOrders = getUntrackedOrders();
		System.out.println("Loaded " + untrackedOrders.size() + " untracked orders");

		final List<RunnableFuture<TrackingEntry>> futures = new ArrayList<>();
		for(final ProcessedOrder order : untrackedOrders) {
			final RunnableFuture<TrackingEntry> trackingNumber = TrackingManager.get().getTrackingNum(order);
			futures.add(trackingNumber);
			System.out.println("Created future tracking update task for processed order " + order.id);
		}

		System.out.println("Starting tracking info update tasks...");
		for(int i = 0; i < futures.size(); i++) {
			final int index = i;
			System.out.println("\tstarting task #" + i);
			taskStarter.execute(() -> futures.get(index).run());
		}

		final Statement st = VDSDBManager.get().createStatement();
		for(int i = 0; i < futures.size(); i++) {
			try {
				System.out.println("Checking status of inventory update task #" + i + "...");
				final TrackingEntry entry = futures.get(i).get();
				final String updateSql = getUpdateTrackingNumberInDBQuery(untrackedOrders.get(i), entry);
				if(updateSql != null) {
					st.addBatch(updateSql);
				}
				EbayCalls.setShipmentTrackingInfo(untrackedOrders.get(i), entry);
				System.out.println("Successfully executed tracking update task for processed order " + untrackedOrders.get(i).id);
			} catch(final Exception e) {
				DBLogging.high(getClass(), "failed to update order tracking for processed order " + untrackedOrders.get(i).id, e);
				System.out.println("Failed to execute tracking update task for processed order " + untrackedOrders.get(i).id);
			}
		}

		try {
			System.out.println("Executed batch of " + st.executeBatch().length + " order tracking updates.");
		} catch(final SQLException e) {
			e.printStackTrace();
		}

		TrackingManager.get().endCycle();
	}

	private List<ProcessedOrder> getUntrackedOrders() {
		final List<ProcessedOrder> orders = new ArrayList<>();
		try {
			final Statement st = VDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT * FROM processed_orders WHERE tracking_number IS NULL");
			while(res.next()) {
				final ProcessedOrder order = new ProcessedOrder.Builder()
					.id(res.getInt("id"))
					.customer_order_id(res.getInt("customer_order_id"))
					.fulfillment_listing_id(res.getInt("fulfillment_listing_id"))
					.fulfillment_transaction_id(res.getString("fulfillment_transaction_id"))
					.tracking_number(res.getString("tracking_number"))
					.sale_price(res.getDouble("sale_price"))
					.build();

				orders.add(order);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return orders;
	}

	private String getUpdateTrackingNumberInDBQuery(final ProcessedOrder order, final TrackingEntry entry) {
		if(order.tracking_number == null || !order.tracking_number.equals(entry.trackingNumber)) {
			return "UPDATE processed_orders SET tracking_number='"+entry.trackingNumber+"',order_status='"+entry.status
					+ "' WHERE id="+order.id;
		}

		return null;
	}
}
