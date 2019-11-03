package main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing;

public class FulfillmentListing {

	public final int id;
	public final int fulfillment_platform_id;
	public final String item_id;
	public final String listing_title;
	public final String listing_url;
	public final String product_id;
	public final String upc;
	public final String ean;

	private FulfillmentListing(final Builder builder) {
		this.id = builder.id;
		this.fulfillment_platform_id = builder.fulfillment_platform_id;
		this.item_id = builder.item_id;
		this.listing_title = builder.listing_title;
		this.listing_url = builder.listing_url;
		this.product_id = builder.product_id;
		this.upc = builder.upc;
		this.ean = builder.ean;
	}

	public static class Builder {
		private int id;
		private int fulfillment_platform_id;
		private String item_id;
		private String listing_title;
		private String listing_url;
		private String product_id;
		private String upc;
		private String ean;
		
		public Builder id(final int id) {
			this.id = id;
			return this;
		}

		public Builder fulfillment_platform_id(final int id) {
			this.fulfillment_platform_id = id;
			return this;
		}

		public Builder item_id(final String id) {
			this.item_id = id;
			return this;
		}

		public Builder listing_title(final String title) {
			this.listing_title = title;
			return this;
		}

		public Builder listing_url(final String url) {
			this.listing_url = url;
			return this;
		}
		
		public Builder product_id(final String id) {
			this.product_id = id;
			return this;
		}
		
		public Builder upc(final String upc) {
			this.upc = upc;
			return this;
		}
		
		public Builder ean(final String ean) {
			this.ean = ean;
			return this;
		}

		public FulfillmentListing build() {
			return new FulfillmentListing(this);
		}
	}

}
