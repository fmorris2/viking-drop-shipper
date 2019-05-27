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
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;

public class EbayInventoryUpdater implements AutomaticInventoryUpdater {

	//576 seconds minimum between updates for a specific listing. eBay caps revisions at 150 per day for a listing
	private static final int MIN_UPDATE_TIME_THRESH = 576_000;

	private static final Map<String, Long> updateCache = new HashMap<>();

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
			if(isOnCooldown(listing)) {
				System.out.println("eBay listing " + listing.id + " is on cooldown.");
				return true;
			}

			System.out.println("Updating inventory for eBay listing " + listing);
			final Map<String, Integer> skuStocks = new HashMap<>();
			final List<FulfillmentListing> fulfillmentListings = FulfillmentManager.get().getListingsForMarketplaceListing(listing.id);
			for(final FulfillmentListing fulfillmentListing : fulfillmentListings) {
				System.out.println("Compiling inventory counts for fulfillment listing " + fulfillmentListing.id);
				final Collection<SkuInventoryEntry> entries = /*Collections.singletonList(new SkuInventoryEntry(null, 0));*/FulfillmentStockManager.getStock(listing, fulfillmentListing).get();
				System.out.println("SkuInventoryEntries: " + entries.size());
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
			System.out.println("Attempting to send inventory update to eBay for listing " + listing.id);
			return sendInventoryUpdateToEbay(listing, skuStocks);
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to update inventory for marketplace listing " + listing + ": ", e);
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
			updateCache.put(listing.listingId, System.currentTimeMillis());
			return true;
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to send inventory update to ebay: ", e);
		}

		return false;
	}

	private boolean isOnCooldown(final MarketplaceListing listing) {
		return System.currentTimeMillis() - updateCache.getOrDefault(listing.listingId, 0L) < MIN_UPDATE_TIME_THRESH;
	}

	@Override
	public void endUpdateCycle() {
	}

}
