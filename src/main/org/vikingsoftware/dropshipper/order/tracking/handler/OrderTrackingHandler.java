package main.org.vikingsoftware.dropshipper.order.tracking.handler;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;

public interface OrderTrackingHandler {
	public boolean prepareToTrack();
	public Optional<String> getTrackingNumber(final ProcessedOrder order);
	public void finishTracking();
}
