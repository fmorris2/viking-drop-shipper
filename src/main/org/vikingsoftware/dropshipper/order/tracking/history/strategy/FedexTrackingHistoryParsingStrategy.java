package main.org.vikingsoftware.dropshipper.order.tracking.history.strategy;

import java.net.URL;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;

public class FedexTrackingHistoryParsingStrategy implements TrackingHistoryParsingStrategy {
	
	private static final String BASE_URL_PREFIX = "https://www.fedex.com/trackingCal/track?data={%22TrackPackagesRequest%22:{%22appType%22:%22WTRK%22,%22appDeviceType%22:%22DESKTOP%22,%22supportHTML%22:true,%22supportCurrentLocation%22:true,%22uniqueKey%22:%22%22,%22processingParameters%22:{},%22trackingInfoList%22:[{%22trackNumberInfo%22:{%22trackingNumber%22:%22";
	private static final String BASE_URL_SUFFIX = "%22,%22trackingQualifier%22:%22%22,%22trackingCarrier%22:%22%22}}]}}&action=trackpackages&locale=en_US&version=1&format=json";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd kk:mm:ss");
	
	@Override
	public List<TrackingHistoryRecord> parse(final ProcessedOrder order) {
		System.out.println("FedexTrackingHistoryParsingStrategy#parse for tracking number: " + order.tracking_number);
		try {
			final String apiUrl = BASE_URL_PREFIX + order.tracking_number + BASE_URL_SUFFIX;
			final URL urlObj = new URL(apiUrl);
			final String apiResponse = IOUtils.toString(urlObj, Charset.forName("UTF-8"));
			final JSONObject json = new JSONObject(apiResponse);
			
			final JSONArray scanEventList = json
					.getJSONObject("TrackPackagesResponse")
					.getJSONArray("packageList")
					.getJSONObject(0)
					.getJSONArray("scanEventList");
			
			return getTrackingHistoryRecords(order, scanEventList);
			
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return new ArrayList<>();
	}
	
	private List<TrackingHistoryRecord> getTrackingHistoryRecords(final ProcessedOrder order, final JSONArray scanEventList) {
		final List<TrackingHistoryRecord> events = new ArrayList<>();
		for(int i = 0; i < scanEventList.length(); i++) {
			final JSONObject scanEvent = scanEventList.getJSONObject(i);
			final long scanEventTimeMs = getScanEventTimeMs(scanEvent);
			final String details = scanEvent.getString("scanDetails");
			final String statusStr = scanEvent.getString("status");
			final TrackingStatus status = mapScanEventToTrackingStatus(scanEvent);
			
			final String[] locParts = scanEvent.getString("scanLocation").split(",");
			String city = null;
			String state = null;
			if(locParts.length > 1) {
				city = locParts[0];
				state = locParts[1].trim();
			}
			
			final TrackingHistoryRecord record = new TrackingHistoryRecord.Builder()
					.processed_order_id(order.id)
					.tracking_status(status)
					.tracking_status_date(scanEventTimeMs)
					.tracking_status_details(statusStr + ((details == null || details.isEmpty()) ? "" : " - " + details))
					.tracking_location_city(city)
					.tracking_location_state(state)
					.build();
			
			if(!events.contains(record)) {
				events.add(record);
			}
		}
		
		Collections.sort(events, (r1, r2) -> (int)(r1.tracking_status_date - r2.tracking_status_date));
		
		int numEventsToRemove = order.currentNumTrackingHistoryEvents;
		while(numEventsToRemove > 0 && !events.isEmpty()) {
			events.remove(0);
			numEventsToRemove--;
		}
		return events;
	}
	
	private long getScanEventTimeMs(final JSONObject scanEvent) {
		final String dateStr = scanEvent.getString("date");
		final String timeStr = scanEvent.getString("time");
		final LocalDateTime dateTime = LocalDateTime.parse(dateStr + " " + timeStr, DATE_FORMAT);
		final ZoneId zoneId = ZoneId.of("GMT"+scanEvent.getString("gmtOffset"));
		final Instant instant = Instant.now();
		final ZoneOffset offset = instant.atZone(zoneId).getOffset();
		final long dateTimeMs = dateTime.toInstant(offset).toEpochMilli();
		return dateTimeMs;
	}
	
	private TrackingStatus mapScanEventToTrackingStatus(final JSONObject scanEvent) {
		if(scanEvent.getBoolean("isDelivered")) {
			return TrackingStatus.DELIVERED;
		}
		
		final String statusCD = scanEvent.getString("statusCD");
		switch(statusCD) {
			case "PU": //picked up
			case "AR": //arrived at FedEx location
			case "DP": //departed FedEx location
			case "OD": //out for delivery
				return TrackingStatus.TRANSIT;
			case "DL":
				return TrackingStatus.DELIVERED;
			case "OC": //shipment information sent to FedEx"
			    return TrackingStatus.PRE_TRANSIT;
		}
		
		return TrackingStatus.UNKNOWN;
	}
}
