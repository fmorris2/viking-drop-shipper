package main.org.vikingsoftware.dropshipper.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class InventoryUpdater implements CycleParticipant {

	private static final int TASK_STARTER_THREADS = 5;

	//marketplace id ==> inventory updater
	private final Map<Integer, AutomaticInventoryUpdater> inventoryUpdaters = new HashMap<>();
	private final ExecutorService taskStarter = Executors.newFixedThreadPool(TASK_STARTER_THREADS);

	private List<MarketplaceListing> listings;
	
	@Override
	public void cycle() {
		MarketplaceLoader.loadMarketplaces();
		SkuMappingManager.load();
		populateInventoryUpdaters();
		if(!setupInventoryUpdaters()) {
			return;
		}
		FulfillmentManager.get().load();
		listings = generateListings();
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
		System.out.println("Starting inventory update process for " + listings.size() + " listings...");
		final List<RunnableFuture<Boolean>> updates = new ArrayList<>();
		for(final MarketplaceListing listing : listings) {
			final AutomaticInventoryUpdater updater = inventoryUpdaters.get(listing.marketplaceId);
			updates.add(updater.updateInventory(listing));
		}

		System.out.println("Starting inventory update tasks...");
		for(int i = 0; i < updates.size(); i++) {
			final int index = i;
			taskStarter.execute(() -> updates.get(index).run());
		}

		for(int i = 0; i < updates.size(); i++) {
			boolean successfulUpdate = false;
			try {
				System.out.println("Checking status of inventory update task #" + i + "...");
				successfulUpdate = updates.get(i).get();
			} catch(final Exception e) {
				DBLogging.high(getClass(), "failed to check inventory update task: ", e);
			}
			if(successfulUpdate) {
				System.out.println("Successfully updated inventory for listing " + listings.get(i));
			} else {
				System.out.println("Failed to update inventory for listing " + listings.get(i));
			}
		}
	}

	private List<MarketplaceListing> generateListings() {
		final List<MarketplaceListing> toReturn = new ArrayList<>();
		for(final Marketplaces market : Marketplaces.values()) {
			toReturn.addAll(market.getMarketplace().getMarketplaceListings());
		}

		return toReturn;
	}

}
