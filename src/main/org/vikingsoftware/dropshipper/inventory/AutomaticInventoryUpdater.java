package main.org.vikingsoftware.dropshipper.inventory;

import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public interface AutomaticInventoryUpdater {
	public boolean prepareForUpdateCycle();
	public Future<Boolean> updateInventory(final MarketplaceListing listing);
	public void endUpdateCycle();
}
