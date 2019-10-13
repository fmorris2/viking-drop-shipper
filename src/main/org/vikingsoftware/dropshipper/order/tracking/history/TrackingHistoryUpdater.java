package main.org.vikingsoftware.dropshipper.order.tracking.history;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shippo.model.Track;
import com.shippo.model.Track.Address;
import com.shippo.model.Track.TrackingEvent;
import com.shippo.model.Track.TrackingStatus;

import main.mysterytracking.TrackingNumber;
import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.shippo.ShippoApiContextManager;
import main.org.vikingsoftware.dropshipper.core.shippo.ShippoCarrier;

public class TrackingHistoryUpdater implements CycleParticipant {

	private static final long CYCLE_TIME = 60_000 * 30;
	
	private long lastCycle;
	
	public static void main(final String[] args) {
		new TrackingHistoryUpdater().cycle();
	}
	
	@Override
	public boolean shouldCycle() {
		return System.currentTimeMillis() - lastCycle >= CYCLE_TIME;
	}
	
	@Override
	public void cycle() {
		System.out.println("Running cycle of TrackingHistoryUpdater");
		lastCycle = System.currentTimeMillis();
		
		final List<ProcessedOrder> orders = getInProgressOrders();		
		final Map<ProcessedOrder, Track> statuses = getShippoStatuses(orders);
		updateTrackingHistoryTable(statuses);
	}
	
	private List<ProcessedOrder> getInProgressOrders() {
		final List<ProcessedOrder> orders = new ArrayList<>();
		
		final Statement st = VSDSDBManager.get().createStatement();
		try {
			final ResultSet res = st.executeQuery("SELECT processed_order.id,tracking_number FROM processed_order "
					+ "LEFT JOIN tracking_history ON processed_order.id = tracking_history.processed_order_id "
					+ "WHERE is_cancelled = 0 AND tracking_number IS NOT NULL AND tracking_status IS NULL OR tracking_status = 2 "
					+ "GROUP BY processed_order.id "
					+ "ORDER BY processed_order.id ASC, tracking_history.id DESC");
			
			while(res.next()) {
				orders.add(new ProcessedOrder.Builder()
						.id(res.getInt("id"))
						.tracking_number(res.getString("tracking_number"))
						.build());
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		System.out.println("Loaded " + orders.size() + " orders which need tracking history updated.");
 		return orders;
	}
	
	private Map<ProcessedOrder, Track> getShippoStatuses(final List<ProcessedOrder> orders) {
		final Map<ProcessedOrder, Track> statuses = new HashMap<>();
		
		for(final ProcessedOrder order : orders) {
			if(order.tracking_number != null) {
				final String shippoCarrierToken = getShippoCarrierToken(order.tracking_number);
				if(shippoCarrierToken != null) {
					try {
						final Track track = Track.getTrackingInfo(shippoCarrierToken, order.tracking_number, ShippoApiContextManager.getLiveKey());
						if(track != null) {
							statuses.put(order, track);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return statuses;
	}
	
	private String getShippoCarrierToken(final String trackingNumber) {
		final TrackingNumber carrierDetails = TrackingNumber.parse(trackingNumber);
		if(!carrierDetails.isCourierRecognized()) {
			System.out.println("Unknown courier for tracking number: " + trackingNumber);
			return null;
		}
		
		final ShippoCarrier shippoCarrier = ShippoCarrier.getCarrier(carrierDetails.getCourierParentName());
		return shippoCarrier == null ? null : shippoCarrier.apiToken;
	}
	
	private void updateTrackingHistoryTable(final Map<ProcessedOrder, Track> statuses) {
		final List<String> insertQueries = new ArrayList<>();
		for(final Map.Entry<ProcessedOrder, Track> entry : statuses.entrySet()) {
			final TrackingEvent evt = entry.getValue().getTrackingStatus();
			if(evt == null) {
				continue;
			}
			final TrackingStatus updatedStatus = evt.getStatus();
			final long ms = entry.getValue().getTrackingStatus().getStatusDate().getTime();
			final Statement st = VSDSDBManager.get().createStatement();
			try {
				final ResultSet res = st.executeQuery("SELECT tracking_status,tracking_status_date FROM tracking_history "
						+ "WHERE processed_order_id="+entry.getKey().id + " ORDER BY id DESC LIMIT 1");
				if(!res.next() || res.getInt("tracking_status") != updatedStatus.ordinal() ||
						res.getLong("tracking_status_date") != ms) {
					System.out.println("New update to tracking history for processed order " + entry.getKey().id);
					final String insertQuery = generateInsertQuery(entry.getKey(), entry.getValue());
					if(insertQuery != null) {
						System.out.println(insertQuery);
						insertQueries.add(insertQuery);
					}
				}
			} catch(final SQLException e) {
				e.printStackTrace();
			}
		}
		
		final boolean success = sendInsertQueries(insertQueries);
		if(success) {
			System.out.println("Successfully processed tracking history update queries.");
			updateCompletedOrders(statuses);
		} else {
			System.out.println("Failed to process tracking history updates.");
		}
	}
	
	private void updateCompletedOrders(final Map<ProcessedOrder, Track> statuses) {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			for(final Map.Entry<ProcessedOrder, Track> entry : statuses.entrySet()) {
				final TrackingEvent evt = entry.getValue().getTrackingStatus();
				if(evt == null || evt.getStatus() == null) {
					continue;
				}
			}
			
			st.executeBatch();
		} catch(final SQLException e) {
			e.printStackTrace();
		}
	}
	
	private String generateInsertQuery(final ProcessedOrder order, final Track status) {
		final TrackingEvent evt = status.getTrackingStatus();
		final Address loc = evt.getLocation();
		final String city = cleanse(getAddressField(loc, "city"));
		final String state = cleanse(getAddressField(loc, "state"));
		final String zip = cleanse(getAddressField(loc, "zip"));
		final String country = cleanse(getAddressField(loc, "country"));
		final String objId = evt.getObjectId();
		final TrackingStatus statusObj = evt.getStatus();
		final long statusDate = evt.getStatusDate().getTime();
		System.err.println("statusObj: " + statusObj + ", evt: " + evt);
		if(statusObj == null) {
			return null;
		}
		return "INSERT INTO tracking_history(processed_order_id,shippo_object_id,"
				+ "tracking_status,tracking_status_date,tracking_status_details,tracking_location_city,"
				+ "tracking_location_state,tracking_location_zip,tracking_location_country) VALUES('"
				+ order.id + "','" + objId + "','" + statusObj.ordinal()
				+ "','" + statusDate + "','" + evt.getStatusDetails() + "','" + city + "','"
				+ state + "','" + zip + "','" + country + "')";
	}
	
	private String cleanse(final String str) {
		return str == null ? "?" : str.replace("'", "");
	}
	
	private String getAddressField(final Address addr, final String field) {
		try {
			final Field privateStringField = Address.class.getDeclaredField(field);
			privateStringField.setAccessible(true);
			final Object obj = privateStringField.get(addr);
			return obj == null ? null : (String)obj;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private boolean sendInsertQueries(final List<String> insertQueries) {
		System.out.println("Sending " + insertQueries.size() + " new tracking history updates...");
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			for(final String query : insertQueries) {
				st.addBatch(query);
			}
			
			final int[] results = st.executeBatch();
			System.out.println("Sent tracking history updates successfully.");
			return Arrays.stream(results).allMatch(i -> i == 1);
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}

}
