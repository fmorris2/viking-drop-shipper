package main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy;

import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.ShippingEstimation;

public interface ShippingEstimatorStrategy {
	
	public Optional<ShippingEstimation> generateEstimation(final FulfillmentListing fulfillmentListing);
}
