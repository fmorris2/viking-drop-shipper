package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.OrderTrackingHandler;

public class AliExpressOrderTrackingHandler implements OrderTrackingHandler {

	@Override
	public Optional<TrackingEntry> getTrackingInfo(ProcessedOrder order) {
		return Optional.empty();
	}


}
