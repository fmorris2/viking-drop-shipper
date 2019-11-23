package main.org.vikingsoftware.dropshipper.order.tracking.history.strategy;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;

public class FedexTrackingHistoryParsingStrategy implements TrackingHistoryParsingStrategy {

	@Override
	public TrackingHistoryRecord parse(final ProcessedOrder order) {
		return null;
	}

}
