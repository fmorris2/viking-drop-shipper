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

	//1000 seconds minimum between updates for a specific listing. eBay caps revisions at 150 per day for a listing
	private static final int MIN_UPDATE_TIME_THRESH = 576_000;

	private static final Map<String, Long> updateCache = new HashMap<>();

	@Override
	public boolean prepareForUpdateCycle() {
		return true;
	}

	@Override
	public RunnableFuture<Boolean> updateInventory(final MarketplaceListing listing) {
		return new FutureTask<>(() -> {
			System.out.println("about to run updateImpl in EbayInventoryUpdater...");
			final boolean success = updateImpl(listing);
			System.out.println("done running updateImpl in EbayInventoryUpdater: " + success);
			return success;
		});
	}

	private boolean updateImpl(final MarketplaceListing listing) {
		try {
			if(isOnCooldown(listing)) {
				System.out.println("eBay listing " + listing.id + " is on cooldown.");
				return true;
			}

			System.out.println("Updating inventory for eBay listing " + listing);
			final Map<String, Integer> skuStocks = new HashMap<>();
			if(!listing.active) {
				skuStocks.put(null, 0);
			} else {
				final List<FulfillmentListing> fulfillmentListings = FulfillmentManager.get().getListingsForMarketplaceListing(listing.id);
				for(final FulfillmentListing fulfillmentListing : fulfillmentListings) {
					System.out.println("Compiling inventory counts for fulfillment listing " + fulfillmentListing.id);
					final Collection<SkuInventoryEntry> entries = //Collections.singletonList(new SkuInventoryEntry(null, 0));
							FulfillmentStockManager.getStock(listing, fulfillmentListing).get();
					System.out.println("SkuInventoryEntries: " + entries.size());
					for(final SkuInventoryEntry entry : entries) {
						int currentStock = skuStocks.getOrDefault(entry.sku, 0);
						currentStock += entry.stock;
						skuStocks.put(entry.sku, currentStock);
					}
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
			/*
			 * There is no need to continue sending updates to eBay & our DB
			 * if the listing is currently inactive, and the last inventory update
			 * we sent to eBay was 0. This if statement cuts down on extra unnecessary
			 * processing
			 */
			if(!listing.active && listing.current_ebay_inventory == 0) {
				System.out.println("No need to send updated for inactive listing.");
				return true;
			}
			
			System.out.println("Sending " + skuStocks.size() + " item stock updates to eBay");
			final List<SkuInventoryEntry> entries = skuStocks.entrySet()
					.stream()
					.map(entry -> new SkuInventoryEntry(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			
			final long parsedStock = entries.stream().map(entry -> entry.stock).count();
			
			if(listing.current_ebay_inventory > 0 && parsedStock > 0) {
				System.out.println("eBay still has inventory - No need to update.");
				return true;
			} else if(listing.current_ebay_inventory <= 0 && parsedStock == 0) {
				System.out.println("Parsed stock was 0 and eBay inventory is currently 0. No need to update");
				return true;
			}
			
			if(EbayCalls.updateInventory(listing.listingId, entries)) {
				System.out.println("successfully sent inventory update to ebay - Updating our DB with last inv update.");
				listing.setCurrentEbayInventory(parsedStock);
			} else {
				System.out.println("did not send inventory update to ebay successfully - Updating our DB accordingly.");
				listing.setCurrentEbayInventory(0);
			}
			
			System.out.println("Our DB has been updated.");
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
