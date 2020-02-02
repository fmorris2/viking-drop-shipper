package main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy.impl;

import java.util.Optional;

import org.apache.log4j.Logger;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.ShippingEstimation;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy.ShippingEstimatorStrategy;

public class SamsClubShippingEstimatorStrategy implements ShippingEstimatorStrategy {
	
	private static final Logger log = Logger.getLogger(SamsClubShippingEstimatorStrategy.class);

	@Override
	public Optional<ShippingEstimation> generateEstimation(final FulfillmentListing fulfillmentListing) {
		log.info("Generating Sams Club shipping estimation for fulfillment listing w/ id " + fulfillmentListing.id);
		
		return Optional.empty();
	}

}
