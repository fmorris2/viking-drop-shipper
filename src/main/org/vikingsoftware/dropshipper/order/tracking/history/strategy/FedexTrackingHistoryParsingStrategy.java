package main.org.vikingsoftware.dropshipper.order.tracking.history.strategy;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;

public class FedexTrackingHistoryParsingStrategy implements TrackingHistoryParsingStrategy {
	
	private static final String BASE_URL = "https://www.fedex.com/trackingCal/track";
	private static final String DATA_PREFIX = "{\"TrackPackagesRequest\":{\"appType\":\"WTRK\",\"appDeviceType\":\"DESKTOP\",\"supportHTML\":true,\"supportCurrentLocation\":true,\"uniqueKey\":\"\",\"processingParameters\":{},\"trackingInfoList\":[{\"trackNumberInfo\":{\"trackingNumber\":\"";
	private static final String DATA_SUFFIX = "\",\"trackingQualifier\":\"\",\"trackingCarrier\":\"\"}}]}}&action=trackpackages&locale=en_US&version=1&format=json";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd kk:mm:ss");
	
	@Override
	public List<TrackingHistoryRecord> parse(final ProcessedOrder order) {
		System.out.println("FedexTrackingHistoryParsingStrategy#parse for tracking number: " + order.tracking_number);
		HttpResponse response = null;
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		try {
			final HttpPost req = new HttpPost(BASE_URL);
			final List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("data", DATA_PREFIX + order.tracking_number + DATA_SUFFIX));
			params.add(new BasicNameValuePair("action", "trackpackages"));
			params.add(new BasicNameValuePair("locale", "en_US"));
			params.add(new BasicNameValuePair("version", "1"));
			params.add(new BasicNameValuePair("format", "json"));
			req.setEntity(new UrlEncodedFormEntity(params));
			req.addHeader("Accept-Charset", "utf-8");
			response = client.execute(req);
			final JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
			System.out.println("JSON: " + json);
			
			final JSONArray scanEventList = json
					.getJSONObject("TrackPackagesResponse")
					.getJSONArray("packageList")
					.getJSONObject(0)
					.getJSONArray("scanEventList");
			
			return getTrackingHistoryRecords(order, scanEventList);
		} catch (final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			client.release(response);
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
		
		return events.size() == order.currentNumTrackingHistoryEvents ? Collections.emptyList() : events;
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
