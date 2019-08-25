package main.org.vikingsoftware.dropshipper.pricing.margins;

public class StagnantListing {
	public final int marketplaceListingId;
	public final double currentMargin;
	public final String listingTitle;
	
	public StagnantListing(final int id, final double margin, final String listingTitle) {
		this.marketplaceListingId = id;
		this.currentMargin = margin;
		this.listingTitle = listingTitle;
	}
	
	@Override
	public String toString() {
		return "Stagnant Listing w/ ID [" + marketplaceListingId + "], currentMargin [" + currentMargin + "]"
				+ ", listing title [" + listingTitle + "]";
	}
}
