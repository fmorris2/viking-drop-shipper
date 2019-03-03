package main.org.vikingsoftware.dropshipper.order.tracking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class OrderTracking implements CycleParticipant {

	@Override
	public void cycle() {
		
		if(!TrackingManager.get().prepareForCycle()) {
			System.out.println("Tracking manager failed to prepare for cycle...");
			return;
		}
		
		if(!FulfillmentManager.get().isLoaded()) {
			FulfillmentManager.get().load();
		}
		
		final Collection<ProcessedOrder> untrackedOrders = getUntrackedOrders();
		System.out.println("Loaded " + untrackedOrders.size() + " untracked orders");
		
		final Map<ProcessedOrder, Future<TrackingEntry>> futures = new HashMap<>();
		for(final ProcessedOrder order : untrackedOrders) {
			final Future<TrackingEntry> trackingNumber = TrackingManager.get().getTrackingNum(order);
			futures.put(order, trackingNumber);
		}
		
		final Statement st = VDSDBManager.get().createStatement();
		for(final Entry<ProcessedOrder, Future<TrackingEntry>> entry : futures.entrySet()) {
			try {
				final TrackingEntry trackingEntry = entry.getValue().get();
				if(trackingEntry != null) {
					final String updateSql = getUpdateTrackingNumberInDBQuery(entry.getKey(), trackingEntry);
					if(updateSql != null) {
						st.addBatch(updateSql);
					}
					EbayCalls.setShipmentTrackingInfo(entry.getKey(), trackingEntry);
				}
			} catch(final Exception e) {
				DBLogging.high(getClass(), "failed to update order tracking for order " + entry.getKey(), e);
			}
		}
		
		try {
			System.out.println("Executed batch of " + st.executeBatch().length + " order tracking updates.");
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		TrackingManager.get().endCycle();
	}
	
	private Collection<ProcessedOrder> getUntrackedOrders() {
		final Collection<ProcessedOrder> orders = new ArrayList<>();
		try {
			final Statement st = VDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT * FROM processed_orders WHERE order_status != 'complete'");
			while(res.next()) {
				final ProcessedOrder order = new ProcessedOrder.Builder()
					.id(res.getInt("id"))
					.customer_order_id(res.getInt("customer_order_id"))
					.fulfillment_listing_id(res.getInt("fulfillment_listing_id"))
					.fulfillment_transaction_id(res.getString("fulfillment_transaction_id"))
					.tracking_number(res.getString("tracking_number"))
					.sale_price(res.getDouble("sale_price"))
					.quantity(res.getInt("quantity"))
					.order_status(res.getString("order_status"))
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
