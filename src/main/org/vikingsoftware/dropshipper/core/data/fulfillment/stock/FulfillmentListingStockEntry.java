package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

public class FulfillmentListingStockEntry {
	
	public final int stock;
	public final double price;
	public final int minPurchaseQty;
	public final boolean needsStockUpdate;
	public final boolean needsPriceUpdate;
	public final boolean successfullyParsedDetails;
	
	private FulfillmentListingStockEntry(final Builder builder) {
		this.stock = builder.stock;
		this.price = builder.price;
		this.minPurchaseQty = builder.minPurchaseQty;
		this.needsStockUpdate = builder.needsStockUpdate;
		this.needsPriceUpdate = builder.needsPriceUpdate;
		this.successfullyParsedDetails = builder.successfullyParsedDetails;
	}
	
	public static final class Builder {
		private int stock;
		private double price;
		private int minPurchaseQty;
		private boolean needsStockUpdate;
		private boolean needsPriceUpdate;
		private boolean successfullyParsedDetails;
		
		public Builder stock(final int stock) {
			this.stock = stock;
			return this;
		}
		
		public Builder price(final double val) {
			this.price = val;
			return this;
		}
		
		public Builder minPurchaseQty(final int val) {
			this.minPurchaseQty = val;
			return this;
		}
		
		public Builder needsStockUpdate(final boolean val) {
			this.needsStockUpdate = val;
			return this;
		}
		
		public Builder needsPriceUpdate(final boolean val) {
			this.needsPriceUpdate = val;
			return this;
		}
		
		public Builder successfullyParsedDetails(final boolean val) {
			this.successfullyParsedDetails = val;
			return this;
		}
		
		public FulfillmentListingStockEntry build() {
			return new FulfillmentListingStockEntry(this);
		}
	}
}
