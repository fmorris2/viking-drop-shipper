package main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing;

public class FulfillmentListing {
	
	public final int id;
	public final int fulfillment_platform_id;
	public final String listing_id;
	public final String listing_url;
	public final double listing_max_price;
	public final String listing_options;
	
	private FulfillmentListing(final Builder builder) {
		this.id = builder.id;
		this.fulfillment_platform_id = builder.fulfillment_platform_id;
		this.listing_id = builder.listing_id;
		this.listing_url = builder.listing_url;
		this.listing_max_price = builder.listing_max_price;
		this.listing_options = builder.listing_options;
	}
	
	public static class Builder {		
		private int id;
		private int fulfillment_platform_id;
		private String listing_id;
		private String listing_url;
		private double listing_max_price;
		private String listing_options;
	
		public Builder id(final int id) {
			this.id = id;
			return this;
		}
		
		public Builder fulfillment_platform_id(final int id) {
			this.fulfillment_platform_id = id;
			return this;
		}
		
		public Builder listing_id(final String id) {
			this.listing_id = id;
			return this;
		}
		
		public Builder listing_url(final String url) {
			this.listing_url = url;
			return this;
		}
		
		public Builder listing_max_price(final double price) {
			this.listing_max_price = price;
			return this;
		}
		
		public Builder listing_options(final String json) {
			this.listing_options = json;
			return this;
		}
		
		public FulfillmentListing build() {
			return new FulfillmentListing(this);
		}
	}
	
}
