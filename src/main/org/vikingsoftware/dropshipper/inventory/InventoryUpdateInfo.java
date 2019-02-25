package main.org.vikingsoftware.dropshipper.inventory;

import java.util.Set;

public class InventoryUpdateInfo {
	public final Set<String> criteria;
	public final int stock;
	
	public InventoryUpdateInfo(final Set<String> criteria, final int stock) {
		this.criteria = criteria;
		this.stock = stock;
	}
}
