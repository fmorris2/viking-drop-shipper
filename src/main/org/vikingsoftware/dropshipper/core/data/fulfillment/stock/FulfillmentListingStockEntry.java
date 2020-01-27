package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

public class FulfillmentListingStockEntry {
	
	public final int stock;
	public final double price;
	public final int minPurchaseQty;
	
	public FulfillmentListingStockEntry(final int stock, final double price,
			final int minPurchaseQty) {
		this.stock = stock;
		this.price = price;
		this.minPurchaseQty = minPurchaseQty;
	}
}
