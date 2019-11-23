package main.org.vikingsoftware.dropshipper.order.tracking.history;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;

public interface TrackingHistoryParsingStrategy {
	
	public TrackingHistoryRecord parse(final ProcessedOrder order);
}
