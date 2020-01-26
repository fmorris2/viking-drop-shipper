package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock;

public class FulfillmentListingStockEntry {
	
	public final int stock;
	public final double price;
	
	public FulfillmentListingStockEntry(final int stock, final double price) {
		this.stock = stock;
		this.price = price;
	}
}
