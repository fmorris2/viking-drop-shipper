package main.org.vikingsoftware.dropshipper.inventory;

import java.util.concurrent.RunnableFuture;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public interface AutomaticInventoryUpdater {
	public boolean prepareForUpdateCycle();
	public RunnableFuture<Boolean> updateInventory(final MarketplaceListing listing);
	public void endUpdateCycle();
}
