package main.org.vikingsoftware.dropshipper.order.tracking.handler;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;

public interface OrderTrackingHandler {
	public Optional<TrackingEntry> getTrackingInfo(final ProcessedOrder order);
}
