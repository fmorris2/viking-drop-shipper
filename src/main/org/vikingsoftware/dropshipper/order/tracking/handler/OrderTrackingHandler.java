package main.org.vikingsoftware.dropshipper.order.tracking.handler;

import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;

public interface OrderTrackingHandler {
	public boolean prepareToTrack();
	public Future<TrackingEntry> getTrackingInfo(final ProcessedOrder order);
	public void finishTracking();
}
