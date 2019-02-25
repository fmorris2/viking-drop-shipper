package main.org.vikingsoftware.dropshipper.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentMapping;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public class InventoryUpdater implements CycleParticipant {
	
	//marketplace id ==> inventory updater
	private final Map<Integer, AutomaticInventoryUpdater> inventoryUpdaters = new HashMap<>();
	
	private List<MarketplaceListing> activeListings;
	
	@Override
	public void cycle() {
		MarketplaceLoader.loadMarketplaces();
		populateInventoryUpdaters();
		if(!setupInventoryUpdaters()) {
			return;
		}
		FulfillmentManager.get().load();
		activeListings = generateActiveListings();
		updateListings();
	}
	
	private void populateInventoryUpdaters() {
		for(final Marketplaces market : Marketplaces.values()) {
			inventoryUpdaters.put(market.getMarketplace().id, market.generateInventoryUpdater());
		}
	}
	
	private boolean setupInventoryUpdaters() {
		for(final AutomaticInventoryUpdater updater : inventoryUpdaters.values()) {
			if(!updater.prepareForUpdateCycle()) {
				return false;
			}
		}
		
		return true;
	}
	
	private void updateListings() {
		for(final MarketplaceListing listing : activeListings) {
			final List<FulfillmentMapping> fulfillmentMappings = FulfillmentManager.get().getFulfillmentMappings(listing.marketplaceId);
			final AutomaticInventoryUpdater updater = inventoryUpdaters.get(listing.marketplaceId);
			
		}
	}
	
	private List<MarketplaceListing> generateActiveListings() {
		final List<MarketplaceListing> toReturn = new ArrayList<>();
		for(final Marketplaces market : Marketplaces.values()) {
			toReturn.addAll(market.getMarketplace().getActiveMarketplaceListings());
		}
		
		return toReturn;
	}

}
