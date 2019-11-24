package main.org.vikingsoftware.dropshipper.order.tracking.history.strategy;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;

public class FedexTrackingHistoryParsingStrategy implements TrackingHistoryParsingStrategy {
	
	private static final String BASE_URL_PREFIX = "https://www.fedex.com/trackingCal/track?data={%22TrackPackagesRequest%22:{%22appType%22:%22WTRK%22,%22appDeviceType%22:%22DESKTOP%22,%22supportHTML%22:true,%22supportCurrentLocation%22:true,%22uniqueKey%22:%22%22,%22processingParameters%22:{},%22trackingInfoList%22:[{%22trackNumberInfo%22:{%22trackingNumber%22:%22";
	private static final String BASE_URL_SUFFIX = "%22,%22trackingQualifier%22:%22%22,%22trackingCarrier%22:%22%22}}]}}&action=trackpackages&locale=en_US&version=1&format=json";
	
	@Override
	public List<TrackingHistoryRecord> parse(final ProcessedOrder order) {
		System.out.println("FedexTrackingHistoryParsingStrategy#parse for tracking number: " + order.tracking_number);
//		try {
//			final String apiUrl = BASE_URL_PREFIX + order.tracking_number + BASE_URL_SUFFIX;
//			final URL urlObj = new URL(apiUrl);
//			final String apiResponse = IOUtils.toString(urlObj, Charset.forName("UTF-8"));
//			final JSONObject json = new JSONObject(apiResponse);
//			
//			final JSONArray scanEventList = json
//					.getJSONObject("TrackPackagesResponse")
//					.getJSONArray("packageList")
//					.getJSONObject(0)
//					.getJSONArray("scanEventList");
//			
//			return //getMostRecentTrackingHistoryRecord(order, scanEventList);
//			
//		} catch(final Exception e) {
//			e.printStackTrace();
//		}
		return null;
	}
	
	private TrackingHistoryRecord getMostRecentTrackingHistoryRecord(final ProcessedOrder order, final JSONArray scanEventList) {
		
		return null;
	}
}
