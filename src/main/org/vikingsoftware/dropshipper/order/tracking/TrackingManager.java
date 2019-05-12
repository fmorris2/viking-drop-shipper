package main.org.vikingsoftware.dropshipper.order.tracking;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.impl.AliExpressOrderTrackingHandler;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.impl.CostcoOrderTrackingHandler;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.impl.SamsClubOrderTrackingHandler;

public class TrackingManager {

	private static TrackingManager instance;

	private final AliExpressOrderTrackingHandler aliExpressHandler;
	private final SamsClubOrderTrackingHandler samsClubHandler;
	private final CostcoOrderTrackingHandler costcoHandler;

	private TrackingManager () {
		aliExpressHandler = new AliExpressOrderTrackingHandler();
		samsClubHandler = new SamsClubOrderTrackingHandler();
		costcoHandler = new CostcoOrderTrackingHandler();
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
		results.add(samsClubHandler.prepareToTrack());
		results.add(costcoHandler.prepareToTrack());

		return !results.contains(false);
	}

	public void endCycle() {
		aliExpressHandler.finishTracking();
		samsClubHandler.finishTracking();
		costcoHandler.finishTracking();
	}

	public RunnableFuture<TrackingEntry> getTrackingNum(final ProcessedOrder order) {
		final Optional<FulfillmentListing> fulfillmentListing = FulfillmentManager.get().getListingForProcessedOrder(order);
		if(!fulfillmentListing.isPresent()) {
			return new FutureTask<>(() -> null);
		}

		final FulfillmentPlatforms platform = FulfillmentPlatforms.getById(fulfillmentListing.get().fulfillment_platform_id);
		switch(platform) {
			case ALI_EXPRESS:
				return aliExpressHandler.getTrackingInfo(order);
			case SAMS_CLUB:
				return samsClubHandler.getTrackingInfo(order);
			case COSTCO:
				return costcoHandler.getTrackingInfo(order);
			default:
				return new FutureTask<>(() -> null);
		}
	}
}
