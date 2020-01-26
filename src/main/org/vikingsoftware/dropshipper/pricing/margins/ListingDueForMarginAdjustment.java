package main.org.vikingsoftware.dropshipper.pricing.margins;

public class ListingDueForMarginAdjustment {
	public final int marketplaceListingId;
	public final double currentMargin;
	public final String listingTitle;
	public final MarginAdjustmentType adjustmentType;
	
	public ListingDueForMarginAdjustment(final int id, final double margin, final String listingTitle,
			final MarginAdjustmentType adjustmentType) {
		this.marketplaceListingId = id;
		this.currentMargin = margin;
		this.listingTitle = listingTitle;
		this.adjustmentType = adjustmentType;
	}
	
	@Override
	public String toString() {
		return "ListingDueForMarginAdjustment Listing w/ ID [" + marketplaceListingId + "], currentMargin [" + currentMargin + "]"
				+ ", listing title [" + listingTitle + "], adjustment type [" + adjustmentType + "]";
	}
}
