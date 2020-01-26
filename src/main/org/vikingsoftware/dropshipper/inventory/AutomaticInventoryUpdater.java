package main.org.vikingsoftware.dropshipper.inventory;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public interface AutomaticInventoryUpdater {
	public boolean updateInventory(final MarketplaceListing listing);
}
