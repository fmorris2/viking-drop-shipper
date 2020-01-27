package main.org.vikingsoftware.dropshipper.inventory.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentListingStockEntry;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.PriceUtils;
import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;

public class EbayInventoryUpdater implements AutomaticInventoryUpdater {

	//1000 seconds minimum between updates for a specific listing. eBay caps revisions at 150 per day for a listing
	private static final int MIN_UPDATE_TIME_THRESH = 576_000;
	private static final double MARGIN_TOLERANCE = 0.2;
	private static final int NUM_EXECUTION_THREADS = 5;
	private static final int MAX_INVENTORY_CYCLE_RUN_TIME_MINUTES = 60;
	
	private static final Map<String, Long> updateCache = new HashMap<>();
	
	private final ExecutorService threadPool = Executors.newFixedThreadPool(NUM_EXECUTION_THREADS);

	@Override
	public boolean updateInventory(final List<MarketplaceListing> listings) {
		for(final MarketplaceListing listing : listings) {
			threadPool.execute(() -> {
				if(updateInventoryForListing(listing)) {
					System.out.println("Successfully updated inventory for marketplace listing id " + listing.id);
				} else {
					System.out.println("Failed to update inventory for marketplace listing id " + listing.id);
				}
			});
		}
		
		boolean success = false;
		try {
			threadPool.shutdown();
			success = threadPool.awaitTermination(MAX_INVENTORY_CYCLE_RUN_TIME_MINUTES, TimeUnit.MINUTES);
		} catch(final InterruptedException e) {
			e.printStackTrace();
		}
		
		return success;
	}
	
	private boolean updateInventoryForListing(final MarketplaceListing listing) {
		try {
			if(isOnCooldown(listing)) {
				System.out.println("eBay listing " + listing.id + " is on cooldown.");
				return true;
			}

			Optional<FulfillmentListingStockEntry> combinedStockEntry = Optional.empty();
			System.out.println("Updating inventory for eBay listing " + listing);
			if(listing.active) {
				combinedStockEntry = generateFulfillmentListingStockEntryForListing(listing);
			} else if(EbayCalls.getListingStock(listing.listingId).orElse(-1) > 0){
				System.out.println("Setting inactive listing stock to 0...");
				EbayCalls.updateInventory(listing.listingId, 0);
				return true;
			} else {
				System.out.println("Inactive listing already has stock set to 0. Skipping...");
				return true;
			}

			if(combinedStockEntry.isEmpty()) {
				System.out.println("Failed to grab stock for marketplace listing " + listing.id);
				return false;
			}
			System.out.println("Attempting to send inventory update to eBay for listing " + listing.id);
			return sendInventoryUpdateToEbay(listing, combinedStockEntry.get());
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to update inventory for marketplace listing " + listing + ": ", e);
		}

		return false;
	}
	
	private Optional<FulfillmentListingStockEntry> generateFulfillmentListingStockEntryForListing(final MarketplaceListing listing) {
		final List<FulfillmentListing> fulfillmentListings = FulfillmentManager.get().getListingsForMarketplaceListing(listing.id);
		Collection<FulfillmentListingStockEntry> entries = new ArrayList<>();
		for(final FulfillmentListing fulfillmentListing : fulfillmentListings) {
			if(FulfillmentManager.canUpdateInventory(fulfillmentListing.fulfillment_platform_id)) {
				System.out.println("Compiling inventory counts for fulfillment listing " + fulfillmentListing.id);
				final Optional<FulfillmentListingStockEntry> stockEntry = FulfillmentStockManager.getStock(listing, fulfillmentListing);
				if(stockEntry.isPresent()) {
					entries.add(stockEntry.get());
				}
			} else {
				System.out.println("Fulfillment platform inventory is frozen for fulfillment listing " + fulfillmentListing.id
						+ ". Setting stock to 0.");
				entries.add(new FulfillmentListingStockEntry(0, 0D, listing.fulfillment_quantity_multiplier));
			}
			System.out.println("SkuInventoryEntries: " + entries.size());
			int totalStock = 0;
			int maxMinPurchaseQty = -1;
			double maxPrice = -1;
			for(final FulfillmentListingStockEntry entry : entries) {
				if(entry.stock <= 0 || entry.price <= 0) {
					continue;
				}
				totalStock += entry.stock;
				maxPrice = Math.max(maxPrice, entry.price);
				maxMinPurchaseQty = Math.max(maxMinPurchaseQty, entry.minPurchaseQty);
			}
			
			if(maxMinPurchaseQty > -1 && maxPrice > -1) {	
				return Optional.of(new FulfillmentListingStockEntry(totalStock, maxPrice, maxMinPurchaseQty));
			}
		}
		
		return Optional.empty();
	}

	private boolean sendInventoryUpdateToEbay(final MarketplaceListing listing, final FulfillmentListingStockEntry combinedStockEntry) {
		try {
			/*
			 * There is no need to continue sending updates to eBay & our DB
			 * if the listing is currently inactive, and the last inventory update
			 * we sent to eBay was 0. This if statement cuts down on extra unnecessary
			 * processing
			 */
			final int currentEbayInventory = EbayCalls.getListingStock(listing.listingId).orElse(-1);//listing.current_ebay_inventory;
			if(!listing.active) {
				if(currentEbayInventory <= 0) {
					System.out.println("No need to send updated for inactive listing.");
					return true;
				} else if(EbayCalls.updateInventory(listing.listingId, 0)) {
					return listing.setCurrentEbayInventory(0);
				}
			}
	
			autoPrice(listing, combinedStockEntry);
			updateHandlingTime(listing);
			
			int parsedStock = combinedStockEntry.stock;
			
			if(parsedStock < 0) {
				parsedStock = 0;
			}
			
			System.out.println("parsedStock: " + parsedStock);
			
			/*
			 * Here, we do some checks in order to optimize eBay API calls. We don't want to request
			 * the API if we don't need to. Therefore, if we already have the correct stock in our DB,
			 * we should simply return instead of updating the listing on eBay.
			 */
			
			if(currentEbayInventory > 0 && parsedStock > 0) {
				System.out.println("eBay still has inventory - No need to update.");
				return true;
			} else if(currentEbayInventory <= 0 && parsedStock <= 0) {
				System.out.println("Parsed stock was 0 and eBay inventory is currently 0. No need to update");
				return true;
			}
			
			if(EbayCalls.updateInventory(listing.listingId, combinedStockEntry.stock)) {
				System.out.println("successfully sent inventory update to ebay - Updating our DB with last inv update.");
				listing.setCurrentEbayInventory(Math.max(0, Math.min(EbayCalls.FAKE_MAX_QUANTITY, parsedStock)));
			} else {
				System.err.println("did not send inventory update to ebay successfully for id " + listing.id + "!");
			}
			
			System.out.println("Our DB has been updated.");
			updateCache.put(listing.listingId, System.currentTimeMillis());
			return true;
		} catch(final Exception e) {
			/*
			 * Log the exception as a high severity in our DB, to be examined.
			 */
			DBLogging.high(getClass(), "failed to send inventory update to ebay: ", e);
			final String exceptionMsg = e.getMessage().toLowerCase();
			if(exceptionMsg.contains("it looks like this listing is for an item you already have on ebay")) {
				System.out.println("Duplicate listings found on eBay - Flagging for purge examination.");
				MarketplaceListing.flagForPurgeExamination(listing.id);
			}
		}

		return false;
	}
	
	private void updateHandlingTime(final MarketplaceListing listing) {
		if(listing.current_handling_time != listing.target_handling_time) {
			System.out.println("Updating handling time for listing id " + listing.id + " from " + listing.current_handling_time
					+ " --> " + listing.target_handling_time + " days.");
			if(EbayCalls.updateHandlingTime(listing.listingId, listing.target_handling_time)) {
				listing.updateHandlingTimeInDB(listing.target_handling_time);
			}
		}
	}
	
	private void autoPrice(final MarketplaceListing listing, final FulfillmentListingStockEntry combinedStockEntry) {
		try {
			System.out.println("Beginning auto-pricing for listing " + listing);
			final Pair<Double,Double> currentPriceInfo = listing.getCurrentPrice(); //this is an API-optimized call to get price
			System.out.println("stockAndPrice: " + combinedStockEntry.stock + ", " + combinedStockEntry.price);
			final double maxFulfillmentPrice = combinedStockEntry.price * listing.fulfillment_quantity_multiplier;
			
			if(maxFulfillmentPrice <= 0) {
				return;
			}
			
			final double currentProfitMargin = PriceUtils.getMarginPercentage(maxFulfillmentPrice, currentPriceInfo.left + currentPriceInfo.right);
			
			System.out.println("\tcurrent ebay price: " + currentPriceInfo.left + " w/ " + currentPriceInfo.right + " shipping");
			System.out.println("\tmax fulfillment price: " + maxFulfillmentPrice);
			System.out.println("\tcurrent profit margin: " + currentProfitMargin);
			System.out.println("\ttarget profit margin: " + listing.target_margin);
			
			/*
			 * If our DB states we already have the target margin, there is no need to update the listing on eBay
			 * and waste an API call.
			 */
			if(Math.abs(currentProfitMargin - listing.target_margin) > MARGIN_TOLERANCE) {
				final double targetPrice = PriceUtils.getPriceFromMargin(maxFulfillmentPrice, currentPriceInfo.right, listing.target_margin);
				if(targetPrice != currentPriceInfo.left) {
					System.out.println("\tAdjusting pricing to fulfill target margin...");
					System.out.println("\t\tNew target price: " + targetPrice);
					listing.updatePrice(targetPrice);
				}
			} else {
				System.out.println("\tMargin is accurate - No need to update price...");
			}
			
		} catch(final Exception e) {
			e.printStackTrace();
			System.out.println("Failed to execute auto pricing for listing " + listing);
		}
	}

	private boolean isOnCooldown(final MarketplaceListing listing) {
		return System.currentTimeMillis() - updateCache.getOrDefault(listing.listingId, 0L) < MIN_UPDATE_TIME_THRESH;
	}

}
