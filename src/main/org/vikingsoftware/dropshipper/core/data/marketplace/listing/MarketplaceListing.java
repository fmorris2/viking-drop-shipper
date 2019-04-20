package main.org.vikingsoftware.dropshipper.core.data.marketplace.listing;

public class MarketplaceListing {

	public final int id;
	public final int marketplaceId;
	public final String listingId;
	public final String listingTitle;
	public final String listingUrl;
	public final double listingPrice;
	public final double shippingPrice;
	public final double percentageCut;
	public final int fulfillment_quantity_multiplier;

	private MarketplaceListing(final Builder builder) {
		this.id = builder.id;
		this.marketplaceId = builder.marketplaceId;
		this.listingId = builder.listingId;
		this.listingTitle = builder.listingTitle;
		this.listingUrl = builder.listingUrl;
		this.listingPrice = builder.listingPrice;
		this.shippingPrice = builder.shippingPrice;
		this.percentageCut = builder.percentageCut;
		this.fulfillment_quantity_multiplier = builder.fulfillment_quantity_multiplier;
	}

	public static class Builder {
		private int id;
		private int marketplaceId;
		private String listingId;
		private String listingTitle;
		private String listingUrl;
		private double listingPrice;
		private double shippingPrice;
		private double percentageCut;
		private int fulfillment_quantity_multiplier;

		public Builder id(final int id) {
			this.id = id;
			return this;
		}

		public Builder marketplaceId(final int id) {
			this.marketplaceId = id;
			return this;
		}

		public Builder listingId(final String listingId) {
			this.listingId = listingId;
			return this;
		}

		public Builder listingTitle(final String listingTitle) {
			this.listingTitle = listingTitle;
			return this;
		}

		public Builder listingUrl(final String listingUrl) {
			this.listingUrl = listingUrl;
			return this;
		}

		public Builder listingPrice(final double listingPrice) {
			this.listingPrice = listingPrice;
			return this;
		}

		public Builder shippingPrice(final double shippingPrice) {
			this.shippingPrice = shippingPrice;
			return this;
		}

		public Builder percentageCut(final double percentageCut) {
			this.percentageCut = percentageCut;
			return this;
		}

		public Builder fulfillmentQuantityMultiplier(final int mult) {
			this.fulfillment_quantity_multiplier = mult;
			return this;
		}

		public MarketplaceListing build() {
			return new MarketplaceListing(this);
		}
	}
}
