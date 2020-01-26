package main.org.vikingsoftware.dropshipper.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class InventoryUpdater implements CycleParticipant {

	private static final long CYCLE_TIME = 60_000 * 30;
	
	//marketplace id ==> inventory updater
	private final Map<Integer, AutomaticInventoryUpdater> inventoryUpdaters = new HashMap<>();

	private List<MarketplaceListing> listings;
	
	public static void main(final String[] args) {
		final InventoryUpdater inventoryUpdater = new InventoryUpdater();
		while(true) {
			try {
				inventoryUpdater.cycle();
				Thread.sleep(CYCLE_TIME);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void cycle() {
		MarketplaceLoader.loadMarketplaces();
		populateInventoryUpdaters();
		FulfillmentManager.get().load();
		listings = generateListings();
		updateListings();
	}

	private void populateInventoryUpdaters() {
		for(final Marketplaces market : Marketplaces.values()) {
			inventoryUpdaters.put(market.getMarketplace().id, market.generateInventoryUpdater());
		}
	}

	private void updateListings() {
		System.out.println("Starting inventory update process for " + listings.size() + " listings...");
		final Map<Integer, List<MarketplaceListing>> listingsPerMarketplace = new HashMap<>(); //map marketplace id --> listings to update
		for(final MarketplaceListing listing : listings) {
			listingsPerMarketplace.computeIfAbsent(listing.marketplaceId, id -> new ArrayList<>()).add(listing);
		}
		
		for(final Map.Entry<Integer, List<MarketplaceListing>> entry : listingsPerMarketplace.entrySet()) {
			final AutomaticInventoryUpdater updater = inventoryUpdaters.get(entry.getKey());
			try {
				updater.updateInventory(entry.getValue());
			} catch(final Exception e) {
				DBLogging.high(getClass(), "failed to update inventory for marketplace listing: ", e);
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
