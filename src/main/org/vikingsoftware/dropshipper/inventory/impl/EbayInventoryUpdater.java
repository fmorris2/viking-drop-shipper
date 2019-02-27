package main.org.vikingsoftware.dropshipper.inventory.impl;

import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;

public class EbayInventoryUpdater implements AutomaticInventoryUpdater {

	@Override
	public boolean prepareForUpdateCycle() {
		return false;
	}

	@Override
	public Future<Boolean> updateInventory(final MarketplaceListing listing) {
		return null;
	}

	@Override
	public void endUpdateCycle() {
	}

}
