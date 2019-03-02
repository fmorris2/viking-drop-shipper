package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.OrderTrackingHandler;

public class AliExpressOrderTrackingHandler implements OrderTrackingHandler {

	@Override
	public boolean prepareToTrack() {
		return false;
	}

	@Override
	public Optional<String> getTrackingNumber(final ProcessedOrder order) {
		return null;
	}

	@Override
	public void finishTracking() {
	}

}
