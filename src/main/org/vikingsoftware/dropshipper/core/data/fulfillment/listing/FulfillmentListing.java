package main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing;

public class FulfillmentListing {

	public final int id;
	public final int fulfillment_platform_id;
	public final String item_id;
	public final String listing_title;
	public final String listing_url;

	private FulfillmentListing(final Builder builder) {
		this.id = builder.id;
		this.fulfillment_platform_id = builder.fulfillment_platform_id;
		this.item_id = builder.item_id;
		this.listing_title = builder.listing_title;
		this.listing_url = builder.listing_url;
	}

	public static class Builder {
		private int id;
		private int fulfillment_platform_id;
		private String item_id;
		private String listing_title;
		private String listing_url;

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

		public FulfillmentListing build() {
			return new FulfillmentListing(this);
		}
	}

}
