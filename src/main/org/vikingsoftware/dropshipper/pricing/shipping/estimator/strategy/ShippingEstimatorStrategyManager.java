package main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.ShippingEstimation;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy.impl.SamsClubShippingEstimatorStrategy;

public class ShippingEstimatorStrategyManager {
	
	private static final Logger log = Logger.getLogger(ShippingEstimatorStrategyManager.class);
	
	private final Map<FulfillmentPlatforms, ShippingEstimatorStrategy> strategies = new HashMap<>();
	
	public ShippingEstimatorStrategyManager() {
		reset();
	}
	
	public Optional<ShippingEstimation> generateShippingEstimation(final FulfillmentListing listing) {
		log.info("Generating shipping estimation for fulfillment listing w/ id " + listing.id);
		final FulfillmentPlatforms specifiedPlatform = FulfillmentPlatforms.getById(listing.fulfillment_platform_id);
		final ShippingEstimatorStrategy strategy = strategies.get(specifiedPlatform);
		
		if(strategy == null) {
			log.warn("No shipping estimation strategy was found for fulfillment platform " + specifiedPlatform);
			return Optional.empty();
		}
		
		return strategy.generateEstimation(listing);
	}
	
	public void reset() {
		strategies.clear();
		strategies.put(FulfillmentPlatforms.SAMS_CLUB, new SamsClubShippingEstimatorStrategy());
	}
}
