package main.org.vikingsoftware.dropshipper.inventory;

import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public interface AutomaticInventoryUpdater {
	public boolean updateInventory(final List<MarketplaceListing> listings);
}
