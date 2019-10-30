package main.org.vikingsoftware.dropshipper.order.tracking.history;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.shippo.model.Track;
import com.shippo.model.Track.Address;
import com.shippo.model.Track.TrackingEvent;

import main.mysterytracking.TrackingNumber;
import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.shippo.ShippoApiContextManager;
import main.org.vikingsoftware.dropshipper.core.shippo.ShippoCarrier;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;

public class TrackingHistoryUpdater implements CycleParticipant {

	private static final long CYCLE_TIME = 60_000 * 30;
	private static Set<String> SHIPPO_CARRIER_BLACKLIST = new HashSet<>(Arrays.asList(
			ShippoCarrier.ONTRAC.apiToken
	));
	
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
		for(final ProcessedOrder order : orders) {
			final Track shippoStatus = getShippoStatus(order);
			if(shippoStatus != null) {
				updateTrackingHistoryTable(order, shippoStatus);
			} else { //can't get status via shippo
				//TODO ONTRAC?
			}
		}
	}
	
	private List<ProcessedOrder> getInProgressOrders() {
		final List<ProcessedOrder> orders = new ArrayList<>();
		
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery("SELECT processed_order.id,tracking_number FROM processed_order "
					+ "LEFT JOIN tracking_history ON processed_order.id = tracking_history.processed_order_id "
					+ "WHERE is_cancelled = 0 AND tracking_number IS NOT NULL AND tracking_number != 'N/A' AND tracking_status IS NULL OR tracking_status = 2 "
					+ "GROUP BY processed_order.id "
					+ "ORDER BY processed_order.id ASC, tracking_history.id DESC")) {
			
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
 		return orders.stream()
 				.filter(order -> order.tracking_number.equals("1Z5R74F90307367765"))
 				.collect(Collectors.toList());
	}
	
	private Track getShippoStatus(final ProcessedOrder order) {
		System.out.println("TrackingHistoryUpdater#getShippoStatus for tracking number " + order.tracking_number);
		Track status = null;
		
		if(order.tracking_number != null) {
			final String shippoCarrierToken = getShippoCarrierToken(order.tracking_number);
			System.out.println("shippoCarrierToken for tracking number " + order.tracking_number + ": " + shippoCarrierToken);
			if(shippoCarrierToken != null && !SHIPPO_CARRIER_BLACKLIST.contains(shippoCarrierToken)) {
				try {
					final Track track = Track.getTrackingInfo(shippoCarrierToken, order.tracking_number, ShippoApiContextManager.getLiveKey());
					System.out.println("Shippo Track object for tracking number " + order.tracking_number + ": " + track);
					if(track != null) {
						status = track;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return status;
	}
	
	private String getShippoCarrierToken(final String trackingNumber) {
		final TrackingNumber carrierDetails = TrackingNumber.parse(trackingNumber);
		if(!carrierDetails.isCourierRecognized()) {
			System.out.println("Unknown courier for tracking number: " + trackingNumber);
			return null;
		}
		
		System.out.println("Courier name: " + carrierDetails.getCourierParentName());
		final ShippoCarrier shippoCarrier = ShippoCarrier.getCarrier(carrierDetails.getCourierParentName());
		System.out.println("ShippoCarrier: " + shippoCarrier);
		return shippoCarrier == null ? null : shippoCarrier.apiToken;
	}
	
	private void updateTrackingHistoryTable(ProcessedOrder order, Track status) {
		final List<String> insertQueries = new ArrayList<>();
		final TrackingEvent evt = status.getTrackingStatus();
		if(evt == null) {
			return;
		}
		final TrackingStatus updatedStatus = TrackingStatus.getStatusFromValue(evt.getStatus().ordinal());
		final long ms = evt.getStatusDate().getTime();
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery("SELECT tracking_status,tracking_status_date FROM tracking_history "
					+ "WHERE processed_order_id="+order.id + " ORDER BY id DESC LIMIT 1")) {
			if(!res.next() || res.getInt("tracking_status") != updatedStatus.value ||
					res.getLong("tracking_status_date") != ms) {
				System.out.println("New update to tracking history for processed order " + order.id);
				final String insertQuery = generateInsertQuery(order, status);
				if(insertQuery != null) {
					System.out.println(insertQuery);
					insertQueries.add(insertQuery);
				}
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		final boolean success = sendInsertQueries(insertQueries);
		if(success) {
			System.out.println("Successfully processed tracking history update queries.");
		} else {
			System.out.println("Failed to process tracking history updates.");
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
		final TrackingStatus statusObj = TrackingStatus.getStatusFromValue(evt.getStatus().ordinal());
		final long statusDate = evt.getStatusDate().getTime();
		System.err.println("statusObj: " + statusObj + ", evt: " + evt);
		if(statusObj == null) {
			return null;
		}
		return "INSERT INTO tracking_history(processed_order_id,shippo_object_id,"
				+ "tracking_status,tracking_status_date,tracking_status_details,tracking_location_city,"
				+ "tracking_location_state,tracking_location_zip,tracking_location_country) VALUES('"
				+ order.id + "','" + objId + "','" + statusObj.value
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
		try (final Statement st = VSDSDBManager.get().createStatement()) {
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
