package main.org.vikingsoftware.dropshipper.core.data.sku;

public class SkuInventoryEntry {
	public final String sku;
	public final int stock;

	public SkuInventoryEntry(final String sku, final int stock) {
		this.sku = sku;
		this.stock = stock;
	}
}
