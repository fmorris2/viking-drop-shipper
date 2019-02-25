package main.org.vikingsoftware.dropshipper.inventory.impl;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;
import main.org.vikingsoftware.dropshipper.inventory.InventoryUpdateInfo;

public class EbayInventoryUpdater implements AutomaticInventoryUpdater {

	@Override
	public boolean prepareForUpdateCycle() {
		return false;
	}

	@Override
	public boolean updateInventory(final MarketplaceListing listing, final InventoryUpdateInfo info) {
		return false;
	}

	@Override
	public void endUpdateCycle() {
	}

}
