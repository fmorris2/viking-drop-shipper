package main.org.vikingsoftware.dropshipper.order.tracking.history;

import java.util.ArrayList;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.ShippingCarrier;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;

public final class TrackingHistoryParsingManager {
	
	private static TrackingHistoryParsingManager instance;
	
	private TrackingHistoryParsingManager() {
		//singletons can't be instantiated from the outside
	}
	
	public static synchronized TrackingHistoryParsingManager get() {
		
		if(instance == null) {
			instance = new TrackingHistoryParsingManager();
		}
		
		return instance;
	}
	
	public List<TrackingHistoryRecord> parseTrackingHistory(final ProcessedOrder order) {
		
		List<TrackingHistoryRecord> records = new ArrayList<>();
		final ShippingCarrier carrier = ShippingCarrier.getCarrierFromTrackingNum(order.tracking_number);
		if(carrier == null) {
			System.err.println("Could not identify carrier for tracking number " + order.tracking_number);
			//TODO LOG IN DB
		} else if(carrier.trackingHistoryParsingStrategy == null) {
			System.err.println("No tracking history parsing strategy defined for carrier: " + carrier);
			//TODO LOG IN DB
		} else {
			records = carrier.trackingHistoryParsingStrategy.parse(order);
		}
		
		return records;
	}

}
