package main.org.vikingsoftware.dropshipper.order.tracking.history;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
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
	
	public TrackingHistoryRecord parseTrackingHistory(final ProcessedOrder order) {
		
		return null;
	}

}
