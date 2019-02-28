package main.org.vikingsoftware.dropshipper.inventory.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;

public class EbayInventoryUpdater implements AutomaticInventoryUpdater {

	@Override
	public boolean prepareForUpdateCycle() {
		return true;
	}

	@Override
	public RunnableFuture<Boolean> updateInventory(final MarketplaceListing listing) {
		return new FutureTask<>(() -> updateImpl(listing));
	}
	
	private boolean updateImpl(final MarketplaceListing listing) {
		try {
			System.out.println("Updating inventory for eBay listing " + listing);
			final Map<String, Integer> skuStocks = new HashMap<>();
			final List<FulfillmentListing> fulfillmentListings = FulfillmentManager.get().getListingsForMarketplaceListing(listing.id);
			for(final FulfillmentListing fulfillmentListing : fulfillmentListings) {
				System.out.println("Compiling inventory counts for fulfillment listing " + fulfillmentListing.id);
				final Collection<SkuInventoryEntry> entries = FulfillmentStockManager.getStock(listing, fulfillmentListing).get();
				for(final SkuInventoryEntry entry : entries) {
					int currentStock = skuStocks.getOrDefault(entry.sku, 0);
					currentStock += entry.stock;
					skuStocks.put(entry.sku, currentStock);
				}
			}
			
			if(skuStocks.isEmpty()) {
				System.out.println("Failed to grab stock for marketplace listing " + listing.id);
				return false;
			}
			return sendInventoryUpdateToEbay(listing, skuStocks);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private boolean sendInventoryUpdateToEbay(final MarketplaceListing listing, final Map<String, Integer> skuStocks) {
		try {
			System.out.println("Sending " + skuStocks.size() + " item stock updates to eBay");
			final List<SkuInventoryEntry> entries = skuStocks.entrySet()
					.stream()
					.map(entry -> new SkuInventoryEntry(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			
			EbayCalls.updateInventory(listing.listingId, entries);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public void endUpdateCycle() {
	}

}
