package main.org.vikingsoftware.dropshipper.order.tracking;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

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
		
		for(final ProcessedOrder order : untrackedOrders) {
			final Optional<String> trackingNumber = TrackingManager.get().getTrackingNum(order);
			if(trackingNumber.isPresent()) {
				updateTrackingNumber(order, trackingNumber.get());
				//TODO UPDATE EBAY ORDER W/ TRACKING INFO
			}
		}
		
		TrackingManager.get().endCycle();
	}
	
	private Collection<ProcessedOrder> getUntrackedOrders() {
		final Collection<ProcessedOrder> orders = new ArrayList<>();
		try {
			final Statement st = VDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT * FROM processed_orders WHERE tracking_number IS NULL AND order_status != 'complete'");
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
	
	private void updateTrackingNumber(final ProcessedOrder order, final String trackingNumber) {
		
	}

}
