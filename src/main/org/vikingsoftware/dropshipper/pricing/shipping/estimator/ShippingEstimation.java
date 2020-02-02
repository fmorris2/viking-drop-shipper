package main.org.vikingsoftware.dropshipper.pricing.shipping.estimator;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;

public class ShippingEstimation {
	
	public final FulfillmentListing fulfillmentListing;
	public final double estimatedCost;
	
	private ShippingEstimation(final Builder builder) {
		this.fulfillmentListing = builder.fulfillmentListing;
		this.estimatedCost = builder.estimatedCost;
	}
	
	public static final class Builder {
		private FulfillmentListing fulfillmentListing;
		private double estimatedCost;
		
		public Builder fulfillmentListing(final FulfillmentListing listing) {
			this.fulfillmentListing = listing;
			return this;
		}
		
		public Builder estimatedCost(final double estimatedCost) {
			this.estimatedCost = estimatedCost;
			return this;
		}
		
		public ShippingEstimation build() {
			return new ShippingEstimation(this);
		}
	}
	
}
