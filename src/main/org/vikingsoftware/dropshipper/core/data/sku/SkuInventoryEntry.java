package main.org.vikingsoftware.dropshipper.core.data.sku;

public class SkuInventoryEntry {
	public final String sku;
	public final int stock;
	public final double price;

	public SkuInventoryEntry(final String sku, final int stock, final double price) {
		this.sku = sku;
		this.stock = stock;
		this.price = price;
	}
}
