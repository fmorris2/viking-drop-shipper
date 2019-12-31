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
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;
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

		try(final Statement st = VSDSDBManager.get().createStatement()) {
			for(int i = 0; i < futures.size(); i++) {
				try {
					System.out.println("Checking status of inventory update task #" + i + "...");
					final TrackingEntry entry = futures.get(i).get();
					if(entry != null && EbayCalls.setShipmentTrackingInfo(untrackedOrders.get(i), entry)) {
						System.out.println("Successfully executed tracking update task for processed order " + untrackedOrders.get(i).id);
						final String updateSql = getUpdateTrackingNumberInDBQuery(untrackedOrders.get(i), entry);
						if(updateSql != null) {
							st.addBatch(updateSql);
							
							final String trackingHistoryQuery = getTrackingHistoryQuery(untrackedOrders.get(i));
							st.addBatch(trackingHistoryQuery);
						}
					}
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
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		TrackingManager.get().endCycle();
	}

	private List<ProcessedOrder> getUntrackedOrders() {
		final List<ProcessedOrder> orders = new ArrayList<>();
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery("SELECT * FROM processed_order WHERE tracking_number IS NULL AND is_cancelled=0")) {
			while(res.next()) {
				final ProcessedOrder order = new ProcessedOrder.Builder()
					.id(res.getInt("id"))
					.customer_order_id(res.getInt("customer_order_id"))
					.fulfillment_listing_id(res.getInt("fulfillment_listing_id"))
					.fulfillment_account_id(res.getInt("fulfillment_account_id"))
					.fulfillment_transaction_id(res.getString("fulfillment_transaction_id"))
					.build();

				orders.add(order);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return orders;
	}

	private String getUpdateTrackingNumberInDBQuery(final ProcessedOrder order, final TrackingEntry entry) {
		if(entry !=  null && (order.tracking_number == null || !order.tracking_number.equals(entry.trackingNumber))) {
			return "UPDATE processed_order SET tracking_number='"+entry.trackingNumber+"' WHERE id="+order.id;
		}

		return null;
	}
	
	private String getTrackingHistoryQuery(final ProcessedOrder order) {
		return "INSERT INTO tracking_history(processed_order_id,tracking_status,tracking_status_date)"
				+ "VALUES(" + order.id + "," + TrackingStatus.TRACKING_NUMBER_PARSED.value + ","
				+ System.currentTimeMillis() + ")";
	}
}
