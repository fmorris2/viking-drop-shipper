package main.org.vikingsoftware.dropshipper.order.tracking.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;

public class TrackingHistoryUpdater implements CycleParticipant {

	private static final long CYCLE_TIME = 60_000 * 60 * 4;
	
	private static final String IN_PROGRESS_ORDERS_QUERY = "SELECT * FROM (SELECT processed_order.id,tracking_number, "
			+ "count(case when tracking_history.tracking_status != 5 then 1 else null end) as trackCount, "
			+ "count(case when tracking_history.tracking_status IN (1,4) then 1 else null end) as completedStatusCount" +
			" FROM processed_order" + 
			" LEFT JOIN tracking_history ON processed_order.id = tracking_history.processed_order_id" + 
			" WHERE processed_order.is_cancelled=0" + 
			" AND processed_order.is_refunded=0" + 
			" AND tracking_number IS NOT NULL" + 
			" AND tracking_number != 'N/A'" + 
			" GROUP BY processed_order.id" + 
			" ORDER BY processed_order.id ASC, tracking_history.id DESC) t2 WHERE completedStatusCount=0";
	
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
			try {
				final List<TrackingHistoryRecord> trackingHistoryRecords = getTrackingHistoryRecords(order);
				if(!trackingHistoryRecords.isEmpty()) {
					updateTrackingHistoryTable(order, trackingHistoryRecords);
				} else { //wasn't able to get tracking status
					//empty for now
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private List<ProcessedOrder> getInProgressOrders() {
		final List<ProcessedOrder> orders = new ArrayList<>();
		
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery(IN_PROGRESS_ORDERS_QUERY)) {
			
			System.out.println(IN_PROGRESS_ORDERS_QUERY);
			while(res.next()) {
				orders.add(new ProcessedOrder.Builder()
						.id(res.getInt("id"))
						.tracking_number(res.getString("tracking_number"))
						.currentNumTrackingHistoryEvents(res.getInt("trackCount"))
						.build());
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		System.out.println("Loaded " + orders.size() + " orders which need tracking history updated.");
 		return orders.stream()
 				.collect(Collectors.toList());
	}
	
	private List<TrackingHistoryRecord> getTrackingHistoryRecords(final ProcessedOrder order) {
		
		List<TrackingHistoryRecord> records = new ArrayList<>();
		if(order.tracking_number != null) {
			final List<TrackingHistoryRecord> parsedRecords = TrackingHistoryParsingManager.get().parseTrackingHistory(order);
			if(parsedRecords != null) {
				records.addAll(parsedRecords);
			}
		}
		
		return records;
	}
	
	private void updateTrackingHistoryTable(ProcessedOrder order, final List<TrackingHistoryRecord> records) {
		final List<String> insertQueries = new ArrayList<>();
		for(final TrackingHistoryRecord record : records) {
			System.out.println("New update to tracking history for processed order " + order.id);
			final String insertQuery = generateInsertQuery(order, record);
			if(insertQuery != null) {
				System.out.println(insertQuery);
				insertQueries.add(insertQuery);
			}
		}
			
		final boolean success = sendInsertQueries(insertQueries);
		if(success) {
			System.out.println("Successfully processed tracking history update queries.");
		} else {
			System.out.println("Failed to process tracking history updates.");
		}
	}
	
	private String generateInsertQuery(final ProcessedOrder order, final TrackingHistoryRecord record) {
		final String city = record.tracking_location_city;
		final String state = record.tracking_location_state;
		final String zip = record.tracking_location_zip;
		final String country = record.tracking_location_country;
		final String tracking_status_details = record.tracking_status_details;
		final TrackingStatus statusObj = record.tracking_status;
		final long statusDate = record.tracking_status_date;
		if(statusObj == null) {
			return null;
		}
		return "INSERT INTO tracking_history(processed_order_id,"
				+ "tracking_status,tracking_status_date,tracking_status_details,tracking_location_city,"
				+ "tracking_location_state,tracking_location_zip,tracking_location_country) VALUES('"
				+ order.id + "','" + statusObj.value + "','" + statusDate 
				+ "','" + tracking_status_details + "','" + city + "','"
				+ state + "','" + zip + "','" + country + "')";
	}
	
	private boolean sendInsertQueries(final List<String> insertQueries) {
		System.out.println("Sending " + insertQueries.size() + " new tracking history updates...");
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			boolean failure = false;
			for(final String query : insertQueries) {
				try {
					st.execute(query);
				} catch(final Exception e) {
					failure = true;
					e.printStackTrace();
				}
			}
			System.out.println("Sent all tracking updates: " + !failure);
			return failure;
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		return false;
	}

}
