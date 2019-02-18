package main.org.vikingsoftware.dropshipper.core.data.marketplace.listing;

public class MarketplaceListing {
	
	public final int id;
	public final int marketplaceId;
	public final String listingId;
	public final String listingTitle;
	public final String listingUrl;
	public final double listingPrice;
	
	private MarketplaceListing(final Builder builder) {
		this.id = builder.id;
		this.marketplaceId = builder.marketplaceId;
		this.listingId = builder.listingId;
		this.listingTitle = builder.listingTitle;
		this.listingUrl = builder.listingUrl;
		this.listingPrice = builder.listingPrice;
	}
	
	public static class Builder {
		private int id;
		private int marketplaceId;
		private String listingId;
		private String listingTitle;
		private String listingUrl;
		private double listingPrice;
		
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
		
		public MarketplaceListing build() {
			return new MarketplaceListing(this);
		}
	}
}
