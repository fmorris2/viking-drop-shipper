package main.org.vikingsoftware.dropshipper.inventory.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
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

			Pair<Integer, Double> stockAndPrice = null;
			System.out.println("Updating inventory for eBay listing " + listing);
			if(listing.active){
				final List<FulfillmentListing> fulfillmentListings = FulfillmentManager.get().getListingsForMarketplaceListing(listing.id);
				for(final FulfillmentListing fulfillmentListing : fulfillmentListings) {
					Collection<Pair<Integer,Double>> entries = new ArrayList<>();
					if(FulfillmentManager.isFrozen(fulfillmentListing.fulfillment_platform_id)) {
						System.out.println("Fulfillment platform is frozen for fulfillment listing " + fulfillmentListing.id
								+ ". Setting stock to 0.");
						entries.add(new Pair<Integer,Double>(0, 0D));
					} else {
						System.out.println("Compiling inventory counts for fulfillment listing " + fulfillmentListing.id);
						entries = //Collections.singletonList(new SkuInventoryEntry(null, 0));
								FulfillmentStockManager.getStock(listing, fulfillmentListing).get();
					}
					System.out.println("SkuInventoryEntries: " + entries.size());
					int totalStock = 0;
					double maxPrice = -1;
					for(final Pair<Integer,Double> entry : entries) {
						if(entry.left <= 0 || entry.right <= 0) {
							continue;
						}
						totalStock += entry.left;
						maxPrice = Math.max(maxPrice, entry.right);
					}
					
					stockAndPrice = new Pair<>(totalStock, maxPrice);
				}
			} else {
				return false;
			}

			if(stockAndPrice == null) {
				System.out.println("Failed to grab stock for marketplace listing " + listing.id);
				return false;
			}
			System.out.println("Attempting to send inventory update to eBay for listing " + listing.id);
			return sendInventoryUpdateToEbay(listing, stockAndPrice);
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to update inventory for marketplace listing " + listing + ": ", e);
		}

		return false;
	}

	private boolean sendInventoryUpdateToEbay(final MarketplaceListing listing, final Pair<Integer, Double> stockAndPrice) {
		try {
			/*
			 * There is no need to continue sending updates to eBay & our DB
			 * if the listing is currently inactive, and the last inventory update
			 * we sent to eBay was 0. This if statement cuts down on extra unnecessary
			 * processing
			 */
			if(!listing.active) {
				if(listing.current_ebay_inventory <= 0) {
					System.out.println("No need to send updated for inactive listing.");
					return true;
				} else if(EbayCalls.updateInventory(listing.listingId, new Pair<>(0, -1D))) {
					return listing.setCurrentEbayInventory(0);
				}
			}
	
			autoPrice(listing, stockAndPrice);
			
			int parsedStock = stockAndPrice.left;
			
			if(parsedStock < 0) {
				System.err.println("JSoup parsed invalid " + parsedStock + " from metadata for marketplace listing id " + listing.id + ". Assuming stock is 0...");
				parsedStock = 0;
				//return true;
			}
			
			System.out.println("parsedStock: " + parsedStock);
			
			/*
			 * Here, we do some checks in order to optimize eBay API calls. We don't want to request
			 * the API if we don't need to. Therefore, if we already have the correct stock in our DB,
			 * we should simply return instead of updating the listing on eBay.
			 */
			if(listing.current_ebay_inventory > 0 && parsedStock > 0) {
				System.out.println("eBay still has inventory - No need to update.");
				return true;
			} else if(listing.current_ebay_inventory <= 0 && parsedStock <= 0) {
				System.out.println("Parsed stock was 0 and eBay inventory is currently 0. No need to update");
				return true;
			}
			
			if(EbayCalls.updateInventory(listing.listingId, stockAndPrice)) {
				System.out.println("successfully sent inventory update to ebay - Updating our DB with last inv update.");
				listing.setCurrentEbayInventory(Math.max(0, Math.min(EbayCalls.FAKE_MAX_QUANTITY, parsedStock)));
			} else {
				System.err.println("did not send inventory update to ebay successfully!");
			}
			
			System.out.println("Our DB has been updated.");
			updateCache.put(listing.listingId, System.currentTimeMillis());
			return true;
		} catch(final Exception e) {
			/*
			 * Log the exception as a high severity in our DB, to be examined.
			 */
			DBLogging.high(getClass(), "failed to send inventory update to ebay: ", e);
		}

		return false;
	}
	
	private void autoPrice(final MarketplaceListing listing, final Pair<Integer,Double> stockAndPrice) {
		try {
			System.out.println("Beginning auto-pricing for listing " + listing);
			final Pair<Double,Double> currentPriceInfo = listing.getCurrentPrice(); //this is an API-optimized call to get price
			System.out.println("stockAndPrice: " + stockAndPrice.left + ", " + stockAndPrice.right);
			final double maxFulfillmentPrice = stockAndPrice.right * listing.fulfillment_quantity_multiplier;
			
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

	@Override
	public void endUpdateCycle() {
	}

}
