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
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public class InventoryUpdater implements CycleParticipant {
	
	private static final int TASK_STARTER_THREADS = 5;
	
	//marketplace id ==> inventory updater
	private final Map<Integer, AutomaticInventoryUpdater> inventoryUpdaters = new HashMap<>();
	private final ExecutorService taskStarter = Executors.newFixedThreadPool(TASK_STARTER_THREADS);
	
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
		FulfillmentStockManager.reset();
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
		System.out.println("Starting inventory update process for " + activeListings.size() + " active listings...");
		final List<RunnableFuture<Boolean>> updates = new ArrayList<>();
		for(final MarketplaceListing listing : activeListings) {
			final AutomaticInventoryUpdater updater = inventoryUpdaters.get(listing.marketplaceId);
			updates.add(updater.updateInventory(listing));
		}
		
		System.out.println("Starting inventory update tasks...");
		for(int i = 0; i < updates.size(); i++) {
			final int index = i;
			System.out.println("\tstarting task #" + i);
			taskStarter.execute(() -> updates.get(index).run());
		}
		
		for(int i = 0; i < updates.size(); i++) {
			boolean successfulUpdate = false;
			try {
				System.out.println("Checking status of inventory update task #" + i + "...");
				successfulUpdate = updates.get(i).get();
			} catch(final Exception e) {
				e.printStackTrace();
			}
			if(successfulUpdate) {
				System.out.println("Successfully updated inventory for listing " + activeListings.get(i));
			} else {
				System.out.println("Failed to update inventory for listing " + activeListings.get(i));
			}
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