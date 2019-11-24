package main.org.vikingsoftware.dropshipper.order.tracking.history.strategy;

import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;

public class UspsTrackingHistoryParsingStrategy implements TrackingHistoryParsingStrategy {

	@Override
	public List<TrackingHistoryRecord> parse(final ProcessedOrder order) {
		return null;
	}

}
