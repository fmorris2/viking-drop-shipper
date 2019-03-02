package main.org.vikingsoftware.dropshipper.order.tracking;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.impl.AliExpressOrderTrackingHandler;

public class TrackingManager {
	
	private static TrackingManager instance;
	
	private final AliExpressOrderTrackingHandler aliExpressHandler;
	
	private TrackingManager () {
		aliExpressHandler = new AliExpressOrderTrackingHandler();
	}
	
	public static TrackingManager get() {
		if(instance == null) {
			instance = new TrackingManager();
		}
		
		return instance;
	}
	
	public boolean prepareForCycle() {
		final Set<Boolean> results = new HashSet<>();
		results.add(aliExpressHandler.prepareToTrack());
		
		return !results.contains(false);
	}
	
	public void endCycle() {
		aliExpressHandler.finishTracking();
	}
	
	public Optional<String> getTrackingNum(final ProcessedOrder order) {
		final Optional<FulfillmentListing> fulfillmentListing = FulfillmentManager.get().getListingForProcessedOrder(order);
		if(!fulfillmentListing.isPresent()) {
			return Optional.empty();
		}
		
		final FulfillmentPlatforms platform = FulfillmentPlatforms.getById(fulfillmentListing.get().fulfillment_platform_id);
		switch(platform) {
			case ALI_EXPRESS:
				return aliExpressHandler.getTrackingNumber(order);
			default:
				return Optional.empty();		
		}
	}
}
