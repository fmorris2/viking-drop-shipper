package main.org.vikingsoftware.dropshipper.inventory.impl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentListingStockEntry;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.FulfillmentStockManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.PriceUtils;
import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;

public class EbayInventoryUpdater implements AutomaticInventoryUpdater {
	
	private static final int NUM_TOP_SELLING_LISTINGS_CONSIDERED = 200;
	
	private static final String TOP_SELLING_LISTINGS_QUERY = "SELECT marketplace_listing.listing_id " + 
			"FROM marketplace_listing " + 
			"INNER JOIN fulfillment_mapping ON marketplace_listing.id=fulfillment_mapping.marketplace_listing_id " + 
			"INNER JOIN processed_order ON processed_order.fulfillment_listing_id=fulfillment_mapping.fulfillment_listing_id " + 
			"GROUP BY marketplace_listing.listing_id " + 
			"ORDER BY COUNT(*) DESC " + 
			"LIMIT " + NUM_TOP_SELLING_LISTINGS_CONSIDERED;
	
	private static final int MAX_EBAY_STOCK = 5;
	private static final double PRICE_INCREMENT_FOR_STOCK = 10.00;
	
	//1000 seconds minimum between updates for a specific listing. eBay caps revisions at 150 per day for a listing
	private static final int MIN_UPDATE_TIME_THRESH = 576_000;
	private static final double MARGIN_TOLERANCE = 0.2;
	private static final int NUM_EXECUTION_THREADS = 5;
	private static final int MAX_INVENTORY_CYCLE_RUN_TIME_MINUTES = 60;
	
	private static final Map<String, Long> updateCache = new HashMap<>();
	
	private final ExecutorService threadPool = Executors.newFixedThreadPool(NUM_EXECUTION_THREADS);
	public final BiFunction<String,Double,Integer> AMT_IN_STOCK_FORMULA = this::calculateAppropriateStockForPrice;
	private final Set<String> topSellingListings = new HashSet<>();
	
	public EbayInventoryUpdater() {
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery(TOP_SELLING_LISTINGS_QUERY)) {
			while(res.next()) {
				topSellingListings.add(res.getString("listing_id"));
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private int calculateAppropriateStockForPrice(final String listingId, final double price) {
		if(topSellingListings.contains(listingId)) {
			int calculatedStock = MAX_EBAY_STOCK - (int)Math.floor(price / PRICE_INCREMENT_FOR_STOCK);
			return Math.max(calculatedStock, 1);
		}
		
		return 1;
	}

	@Override
	public List<FulfillmentListingStockEntry> updateInventory(final List<MarketplaceListing> listings) {
		final List<FulfillmentListingStockEntry> results = new ArrayList<>();
		
		for(final MarketplaceListing listing : listings) {
			threadPool.execute(() -> {
				final Optional<FulfillmentListingStockEntry> update = updateInventoryForListing(listing);
				if(update.isPresent()) {
					System.out.println("Successfully updated inventory for marketplace listing id " + listing.id);
					results.add(update.get());
				} else {
					System.out.println("Failed to update inventory for marketplace listing id " + listing.id);
				}
			});
		}
		
		try {
			threadPool.shutdown();
			threadPool.awaitTermination(MAX_INVENTORY_CYCLE_RUN_TIME_MINUTES, TimeUnit.MINUTES);
		} catch(final InterruptedException e) {
			e.printStackTrace();
		}
		
		return results;
	}
	
	private Optional<FulfillmentListingStockEntry> updateInventoryForListing(final MarketplaceListing listing) {
		try {
			if(isOnCooldown(listing)) {
				System.out.println("eBay listing " + listing.id + " is on cooldown.");
				return Optional.empty();
			}

			Optional<FulfillmentListingStockEntry> combinedStockEntry = Optional.empty();
			System.out.println("Updating inventory for eBay listing " + listing);
			if(listing.active) {
				System.out.println("Generating FulfillmentListingStockEntry for active listing " + listing.id);
				combinedStockEntry = generateFulfillmentListingStockEntryForListing(listing);
			} else if(EbayCalls.getListingStock(listing.listingId).orElse(-1) > 0){
				System.out.println("Setting inactive listing stock to 0...");
				EbayCalls.updateInventory(listing.listingId, 0);
				final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
						.needsStockUpdate(true)
						.build();
				return Optional.of(entry);
			} else {
				System.out.println("Inactive listing already has stock set to 0. Skipping...");
				final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
						.build();
				return Optional.of(entry);
			}

			if(combinedStockEntry.isEmpty()) {
				System.out.println("Failed to grab stock for marketplace listing " + listing.id);
				return Optional.empty();
			}
			System.out.println("Attempting to send inventory update to eBay for listing " + listing.id);
			return sendInventoryUpdateToEbay(listing, combinedStockEntry.get());
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to update inventory for marketplace listing " + listing + ": ", e);
		}

		return Optional.empty();
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
				final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
						.needsStockUpdate(true)
						.successfullyParsedDetails(false)
						.build();
				
				entries.add(entry);
			}
			System.out.println("SkuInventoryEntries: " + entries.size());
			int totalStock = 0;
			int maxMinPurchaseQty = -1;
			double maxPrice = -1;
			for(final FulfillmentListingStockEntry entry : entries) {
				if(entry.stock < 0 || entry.price < 0) {
					continue;
				}
				totalStock += entry.stock;
				maxPrice = Math.max(maxPrice, entry.price);
				maxMinPurchaseQty = Math.max(maxMinPurchaseQty, entry.minPurchaseQty);
			}
			
			if(maxMinPurchaseQty > -1 && maxPrice > -1) {
				final FulfillmentListingStockEntry entry = new FulfillmentListingStockEntry.Builder()
						.stock(totalStock)
						.price(maxPrice)
						.minPurchaseQty(maxMinPurchaseQty)
						.needsStockUpdate(true)
						.needsPriceUpdate(maxPrice > 0)
						.successfullyParsedDetails(true)
						.build();
				return Optional.of(entry);
			}
		}
		
		return Optional.empty();
	}

	private Optional<FulfillmentListingStockEntry> sendInventoryUpdateToEbay(
			final MarketplaceListing listing, final FulfillmentListingStockEntry combinedStockEntry) {
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
					System.out.println("No need to send update for inactive listing.");
					return Optional.empty();
				} else if(EbayCalls.updateInventory(listing.listingId, 0)) {
					listing.setCurrentEbayInventory(0);
					return Optional.of(combinedStockEntry);
				}
			}
			
			if(combinedStockEntry.successfullyParsedDetails && listing.fulfillment_quantity_multiplier
					!= combinedStockEntry.minPurchaseQty) {
				resolveMinPurchaseQtyInconsistency(listing, combinedStockEntry);
			}
	
			if(combinedStockEntry.needsPriceUpdate) {
				autoPrice(listing, combinedStockEntry);
			}
			
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
			
			final int calculatedStock = Math.min(parsedStock, AMT_IN_STOCK_FORMULA.apply(listing.listingId, combinedStockEntry.price));
			System.out.println("Calculated stock: " + calculatedStock);
			
			if(currentEbayInventory > calculatedStock && parsedStock >= 0) {
				System.out.println("eBay has too much inventory - Decreasing.");
			} else if(currentEbayInventory >= calculatedStock && parsedStock >= 0) {
				System.out.println("eBay still has inventory - No need to update.");
				return Optional.of(combinedStockEntry);
			} else if(currentEbayInventory <= 0 && parsedStock <= 0) {
				System.out.println("Parsed stock was 0 and eBay inventory is currently 0. No need to update");
				return Optional.of(combinedStockEntry);
			}
			
			if(EbayCalls.updateInventory(listing.listingId, calculatedStock)) {
				System.out.println("successfully sent inventory update to ebay - Updating our DB with last inv update.");
				listing.setCurrentEbayInventory(Math.max(0, calculatedStock));
			} else {
				System.err.println("did not send inventory update to ebay successfully for id " + listing.id + "!");
			}
			
			System.out.println("Our DB has been updated.");
			updateCache.put(listing.listingId, System.currentTimeMillis());
			return Optional.of(combinedStockEntry);
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

		return Optional.empty();
	}
	
	private void resolveMinPurchaseQtyInconsistency(final MarketplaceListing listing, 
			final FulfillmentListingStockEntry combinedStockEntry) {
		System.out.println("Resolving minimum purchase quantity inconsistency for listing " + listing.id);
		if(EbayCalls.updateFulfillmentQuantityMultiplier(listing, combinedStockEntry.minPurchaseQty)) {
			System.out.println("Successfully updated eBay listing to resolve minimum purchase quantity inconsistency.");
			MarketplaceListing.setFulfillmentQuantityMultiplier(listing, combinedStockEntry.minPurchaseQty);
		}
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
			
			if(combinedStockEntry.price <= 0) {
				return;
			}
			
			final double currentProfitMargin = PriceUtils.getMarginPercentage(combinedStockEntry.price, currentPriceInfo.left + currentPriceInfo.right);
			
			System.out.println("\tcurrent ebay price: " + currentPriceInfo.left + " w/ " + currentPriceInfo.right + " shipping");
			System.out.println("\tmax fulfillment price: " + combinedStockEntry.price);
			System.out.println("\tcurrent profit margin: " + currentProfitMargin);
			System.out.println("\ttarget profit margin: " + listing.target_margin);
			
			/*
			 * If our DB states we already have the target margin, there is no need to update the listing on eBay
			 * and waste an API call.
			 */
			if(Math.abs(currentProfitMargin - listing.target_margin) > MARGIN_TOLERANCE) {
				final double targetPrice = PriceUtils.getPriceFromMargin(combinedStockEntry.price, currentPriceInfo.right, listing.target_margin);
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
