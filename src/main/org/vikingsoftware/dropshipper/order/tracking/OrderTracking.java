package main.org.vikingsoftware.dropshipper.order.tracking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class OrderTracking implements CycleParticipant {

	@Override
	public void cycle() {
		if(!FulfillmentManager.get().isLoaded()) {
			FulfillmentManager.get().load();
		}

		final List<ProcessedOrder> untrackedOrders = getUntrackedOrders();
		System.out.println("Loaded " + untrackedOrders.size() + " untracked orders");

		try(final Statement st = VSDSDBManager.get().createStatement()) {
			for(final ProcessedOrder order : untrackedOrders) {
				try {
					final Optional<TrackingEntry> trackingNumber = TrackingManager.get().getTrackingNum(order);
					if(trackingNumber.isPresent() && EbayCalls.setShipmentTrackingInfo(order, trackingNumber.get())) {
						System.out.println("Successfully executed tracking update task for processed order " + order.id);
						final String updateSql = getUpdateTrackingNumberInDBQuery(order, trackingNumber.get());
						if(updateSql != null) {
							st.addBatch(updateSql);
							
							final String trackingHistoryQuery = getTrackingHistoryQuery(order);
							st.addBatch(trackingHistoryQuery);
						}
					}
				} catch(final Exception e) {
					DBLogging.high(getClass(), "failed to update order tracking for processed order " + order.id, e);
					System.out.println("Failed to execute tracking update task for processed order " + order.id);
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
