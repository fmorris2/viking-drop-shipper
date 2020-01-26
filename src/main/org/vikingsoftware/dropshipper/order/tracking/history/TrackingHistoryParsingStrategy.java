package main.org.vikingsoftware.dropshipper.order.tracking.history;

import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;

public interface TrackingHistoryParsingStrategy {
	
	public List<TrackingHistoryRecord> parse(final ProcessedOrder order);
}
