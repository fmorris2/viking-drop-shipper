package main.org.vikingsoftware.dropshipper.core.ebay;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.ConnectException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.AddFixedPriceItemCall;
import com.ebay.sdk.call.CompleteSaleCall;
import com.ebay.sdk.call.GetAccountCall;
import com.ebay.sdk.call.GetApiAccessRulesCall;
import com.ebay.sdk.call.GetCategorySpecificsCall;
import com.ebay.sdk.call.GetItemCall;
import com.ebay.sdk.call.GetItemTransactionsCall;
import com.ebay.sdk.call.GetOrdersCall;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.sdk.call.GetSuggestedCategoriesCall;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.sdk.call.ReviseInventoryStatusCall;
import com.ebay.soap.eBLBaseComponents.AccountEntrySortTypeCodeType;
import com.ebay.soap.eBLBaseComponents.AccountEntryType;
import com.ebay.soap.eBLBaseComponents.AccountHistorySelectionCodeType;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.ApiAccessRuleType;
import com.ebay.soap.eBLBaseComponents.BrandMPNType;
import com.ebay.soap.eBLBaseComponents.BuyerPaymentMethodCodeType;
import com.ebay.soap.eBLBaseComponents.BuyerRequirementDetailsType;
import com.ebay.soap.eBLBaseComponents.CategoryType;
import com.ebay.soap.eBLBaseComponents.CountryCodeType;
import com.ebay.soap.eBLBaseComponents.CurrencyCodeType;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.InventoryStatusType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ListingDurationCodeType;
import com.ebay.soap.eBLBaseComponents.ListingTypeCodeType;
import com.ebay.soap.eBLBaseComponents.NameRecommendationType;
import com.ebay.soap.eBLBaseComponents.NameValueListArrayType;
import com.ebay.soap.eBLBaseComponents.NameValueListType;
import com.ebay.soap.eBLBaseComponents.OrderIDArrayType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.PaginationType;
import com.ebay.soap.eBLBaseComponents.PaymentsInformationType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.ProductListingDetailsType;
import com.ebay.soap.eBLBaseComponents.RecommendationValidationRulesType;
import com.ebay.soap.eBLBaseComponents.RecommendationsType;
import com.ebay.soap.eBLBaseComponents.RefundInformationType;
import com.ebay.soap.eBLBaseComponents.ReturnPolicyType;
import com.ebay.soap.eBLBaseComponents.ReturnsAcceptedCodeType;
import com.ebay.soap.eBLBaseComponents.ShipmentTrackingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShipmentType;
import com.ebay.soap.eBLBaseComponents.ShippingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceOptionsType;
import com.ebay.soap.eBLBaseComponents.SiteCodeType;
import com.ebay.soap.eBLBaseComponents.TransactionType;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.UnknownMarketplaceMapping;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.EbayConversionUtils;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.EbayCategory;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public class EbayCalls {

	public static final int FAKE_MAX_QUANTITY = 1;
	
	//view all error codes here: https://developer.ebay.com/devzone/xml/docs/Reference/ebay/Errors/errormessages.htm
	private static final String[] MARK_AS_INACTIVE_ERROR_CODES = {"17", "291", "21916333", "21916750"};
	private static final String[] MARK_AS_PURGED_ERROR_CODES = {"17", "291", "21916333", "21916750"};

	private EbayCalls() {}
	
	public static TransactionType getItemTransaction(final String ebayListingID, final String ebayTransactionId) {
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final GetItemTransactionsCall call = new GetItemTransactionsCall(api);
		call.setItemID(ebayListingID);
		call.setTransactionID(ebayTransactionId);
		try {
			final TransactionType[] transactions = call.getItemTransactions();
			return transactions != null && transactions.length > 0 ? transactions[0] : null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static OrderType getOrder(final String ebayOrderId) {
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final GetOrdersCall call = new GetOrdersCall(api);
		final OrderIDArrayType orderIdArr = new OrderIDArrayType();
		orderIdArr.setOrderID(new String[]{ebayOrderId});
		call.setOrderIDArray(orderIdArr);
		try {
			final OrderType[] orders = call.getOrders();
			return orders != null && orders.length > 0 ? orders[0] : null;
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	public static Optional<Integer> getListingStock(final String ebayListingID) {
		Optional<Integer> stock = Optional.empty();
		
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final GetItemCall call = new GetItemCall(api);
		try {
			call.getItem(ebayListingID);
			final ItemType returnedItem = call.getReturnedItem();
			if(returnedItem != null) {
				final int totalQty = returnedItem.getQuantity(); //qty sold + qty available
				final int totalSold = returnedItem.getSellingStatus().getQuantitySold();
				final int qtyAvailable = totalQty - totalSold;
				stock = Optional.of(qtyAvailable);
			}
		} catch(final ApiException e) {
			handleApiException(e, ebayListingID);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return stock;
	}

	public static CustomerOrder[] getOrdersLastXDays(final int days) {
		try {

			final TransactionType[] transactions = loadTransactionsLastXDays(days);
			if(transactions == null) {
				return new CustomerOrder[0];
			}
			
			final List<CustomerOrder> orders = new ArrayList<>();
			final List<TransactionType> unknownTransactionMappings = new ArrayList<>();
			
			System.out.println("Num eBay transactions in last " + days + " days: " + transactions.length);
			determineRelevantOrders(transactions, orders, unknownTransactionMappings);
			System.out.println("Determined relevant orders...");
			logUnknownMarketplaceMappingsInDB(unknownTransactionMappings);
			System.out.println("Logged unknown marketplace mappings in DB...");
			return orders.toArray(new CustomerOrder[orders.size()]);
		} catch(final ConnectException e) {
			System.err.println("Failed to connect to eBay API to get orders...");
		} catch(final Exception e) {
			DBLogging.high(EbayCalls.class, "failed to get orders last " + days + " days: ", e);
		}

		return new CustomerOrder[0];
	}
	
	private static TransactionType[] loadTransactionsLastXDays(final int days) throws Exception {
		final List<TransactionType> totalTransactions = new ArrayList<>();
		int daysLeft = days;
		final ApiContext apiContext = EbayApiContextManager.getLiveContext();
		while(daysLeft > 0) {
			final GetSellerTransactionsCall call = new GetSellerTransactionsCall(apiContext);
			call.setIncludeFinalValueFee(true);
			call.setDetailLevel(new DetailLevelCodeType[]{DetailLevelCodeType.RETURN_ALL});
			final PaginationType pagination = new PaginationType();
			int page = 1;
			boolean hasResultsOnPage = true;
			while(hasResultsOnPage) {
				pagination.setEntriesPerPage(200);
				pagination.setPageNumber(page);
				call.setPagination(pagination);
				final Calendar from = Calendar.getInstance();
				from.add(Calendar.DAY_OF_YEAR, daysLeft * -1);
				final Calendar to = Calendar.getInstance();
				if(daysLeft > 29) {
					to.add(Calendar.DAY_OF_YEAR, (daysLeft * -1) + 29);
				}
				call.setTimeFilter(new TimeFilter(from, to)); //will use number of days filter
				try {
					final TransactionType[] transactions = call.getSellerTransactions();
					if(transactions.length == 0) {
						hasResultsOnPage = false;
					}
					for(final TransactionType trans : transactions) {
						totalTransactions.add(trans);
					}
					page++;
				} catch(final ApiException e) {
					hasResultsOnPage = false;
				}
			}
			daysLeft -= 29;
		}
		
		return totalTransactions.toArray(new TransactionType[totalTransactions.size()]);
	}
	
	private static void determineRelevantOrders(final TransactionType[] transactions, final List<CustomerOrder> orders,
			final List<TransactionType> unknownTransactionMappings) {
		
		for(final TransactionType trans : transactions) {
			System.out.println("Iterating over transaction: " + trans);
			if(trans == null || trans.getItem() == null || trans.getAmountPaid() == null
					|| trans.getActualShippingCost() == null || trans.getMonetaryDetails() == null) {
				System.out.println("Skipping transaction due to failed conditions...");
				continue;
			}
			
			final PaymentsInformationType monetaryDetails = trans.getMonetaryDetails();
			final RefundInformationType refundType = monetaryDetails.getRefunds();
			System.out.println("Getting marketplaceListingDbId for item ID: " + trans.getItem().getItemID());
			final int marketplaceListingDbId = Marketplaces.EBAY.getMarketplace().getMarketplaceListingIndex(trans.getItem().getItemID());
			if(marketplaceListingDbId == -1) {
				System.out.println("Encountered unknown transaction");
				unknownTransactionMappings.add(trans);
			} else if(refundType != null && refundType.getRefundLength() > 0) { //refunded transaction
				System.out.println("Encountered refunded transaction: " + trans.getTransactionID());
				CustomerOrderManager.setOrderRefunded(marketplaceListingDbId, trans.getTransactionID());
			} else if(trans.getAmountPaid().getValue() <= 0) { //awaiting ebay payment or cancelled
				System.out.println("Encountered transaction with incomplete payment");
				continue;
			} else {
				System.out.println("Converting transaction to customer order...");
				final CustomerOrder order = EbayConversionUtils.convertTransactionTypeToCustomerOrder(marketplaceListingDbId, trans.getItem().getItemID(), trans);
				System.out.println("\tdone.");
				orders.add(order);
			}
		}
		
		System.out.println("EbayCalls#determineRelevantOrders has finished.");
	}

	private static void logUnknownMarketplaceMappingsInDB(final List<TransactionType> transactions) {
		final Set<UnknownMarketplaceMapping> currentDatabaseRecords = UnknownMarketplaceMapping.loadUnknownMarketplaceMappings();
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			int numMappingsToInsert = 0;
			for(final TransactionType trans : transactions) {
				final int marketplace_id = Marketplaces.EBAY.getMarketplaceId();
				final String listing_id = trans.getItem().getItemID();
				final UnknownMarketplaceMapping mappingToInsert = new UnknownMarketplaceMapping(marketplace_id, listing_id);
				if(!currentDatabaseRecords.contains(mappingToInsert)) {
					st.addBatch("INSERT INTO unknown_marketplace_mappings(marketplace_id, listing_id) "
						+ "VALUES("+marketplace_id+", '"+listing_id+"')");
					numMappingsToInsert++;
				}
			}
			if(numMappingsToInsert > 0) {
				System.out.println("Logging " + numMappingsToInsert + " unknown eBay marketplace mappings in DB");
				st.executeBatch();
			}
		} catch(final Exception e) {
			DBLogging.high(EbayCalls.class, "Failed to insert unknown marketplace mappings into DB", e);
		}
	}
	
	public static boolean updatePrice(final String listingId, final double price) {
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final ReviseInventoryStatusCall call = new ReviseInventoryStatusCall(api);
			final InventoryStatusType invStatus = new InventoryStatusType();
			invStatus.setItemID(listingId);
			
			final AmountType priceType = new AmountType();
			priceType.setCurrencyID(CurrencyCodeType.USD);
			priceType.setValue(price);
			invStatus.setStartPrice(priceType);
			
			call.setInventoryStatus(new InventoryStatusType[]{invStatus});
			call.reviseInventoryStatus();
			return !call.hasError();
		} catch(final Exception e) {
			e.printStackTrace();
			
			if(e.getMessage().contains("It looks like this listing is for an item you already have on eBay") ||
				e.getMessage().equals("This item cannot be accessed because the listing has been deleted or you are not the seller.")) {
				System.out.println("setting deleted listing to inactive in database...");
				if(MarketplaceListing.setIsActive(listingId, false)) {
					System.out.println("\tsuccess");
				} else {
					System.out.println("\tfailure");
				}
			}
		}
		
		return false;
	}
	
	public static boolean updateHandlingTime(final String listingId, final int handlingDays) {
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final ReviseFixedPriceItemCall call = new ReviseFixedPriceItemCall(api);
			final ItemType itemToRevise = new ItemType();
			itemToRevise.setItemID(listingId);
			itemToRevise.setDispatchTimeMax(handlingDays);
			
			call.setItemToBeRevised(itemToRevise);
			call.reviseFixedPriceItem();
			return !call.hasError();
		} catch(final ApiException e) {
			handleApiException(e, listingId);
 		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static void handleApiException(final ApiException e, final String listingId) {
		final boolean markAsInactive = exceptionQualifiesForInactiveMarking(e);
		final boolean markAsPurged = exceptionQualifiesForPurgedMarking(e);
		
		if(markAsInactive) {
			System.out.println("Marking listing " + listingId + " as inactive...");
			MarketplaceListing.setIsActive(listingId, false);
		}
		
		if(markAsPurged) {
			System.out.println("Marking listing " + listingId + " as purged...");
			MarketplaceListing.setIsPurged(listingId, true);
		}
		
		if(!markAsInactive && !markAsPurged) {
			e.printStackTrace();
		}
	}
	
	private static boolean exceptionQualifiesForPurgedMarking(final ApiException e) {
		for(final String code : MARK_AS_PURGED_ERROR_CODES) {
			if(e.containsErrorCode(code)) {
				return true;
			}
		}
		
		return false;
	}
	
	private static boolean exceptionQualifiesForInactiveMarking(final ApiException e) {
		for(final String code : MARK_AS_INACTIVE_ERROR_CODES) {
			if(e.containsErrorCode(code)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static Pair<Double, Double> getPrice(String listingId) throws Exception {
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final GetItemCall call = new GetItemCall(api);
		final ItemType item = call.getItem(listingId);
		
		final double price = item.getStartPrice().getValue();
		final double shippingPrice = item.getShippingDetails().getShippingServiceOptions()[0].getShippingServiceCost().getValue();
		
		if(price + shippingPrice <= 0) {
			throw new RuntimeException("Could not parse eBay price for listing " + listingId);
		}
		
		return new Pair<>(price, shippingPrice);
	}
	
	public static Optional<Integer> getHandlingTime(final String listingId) {
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final GetItemCall call = new GetItemCall(api);
		call.setItemID(listingId);
		try {
			final ItemType item = call.getItem();
			return Optional.of(item.getDispatchTimeMax());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Optional.empty();
	}

	public static boolean updateInventory(final String listingId, final int inventory) {
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final ReviseInventoryStatusCall call = new ReviseInventoryStatusCall(api);
			final InventoryStatusType invStatus = new InventoryStatusType();
			invStatus.setItemID(listingId);
			if(inventory < FulfillmentManager.SAFE_STOCK_THRESHOLD) {
				invStatus.setQuantity(0);
			} else {
				invStatus.setQuantity(Math.max(0, Math.min(FAKE_MAX_QUANTITY, inventory)));
			}
			System.out.println("Setting stock for listing id " + listingId + " to " + invStatus.getQuantity());
			call.setInventoryStatus(new InventoryStatusType[] {invStatus});
			call.reviseInventoryStatus();
			logToFile("updateInventory: listingId - " + listingId + ", quantity: " + Math.max(0, Math.min(FAKE_MAX_QUANTITY, invStatus.getQuantity())));
			return !call.hasError();
		} catch(final ApiException e) {
			handleApiException(e, listingId);
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.medium(EbayCalls.class, "failed to update inventory for listing " + listingId + " w/ stock " + inventory+ ": ", e);
		}

		return false;
	}
	
	private static void logToFile(final String str) {
		try(final FileWriter fW = new FileWriter("debug/call-log.txt", true);
			final BufferedWriter bW = new BufferedWriter(fW)) {
			bW.write(str);
			bW.newLine();
			bW.flush();
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(final String[] args) throws Exception {
//		System.out.println(getListingStock("372748630490").orElse(-5));
		checkAPIAccessRules();
//		MarketplaceLoader.loadMarketplaces();
//		final Set<MarketplaceListing> listings = Marketplaces.EBAY.getMarketplace().getMarketplaceListings();
//		for(final MarketplaceListing listing : listings) {
//			updateHandlingTime(listing.listingId, 4);
//		}
		
//		getOrdersLastXDays(2);
		

	}
	
	public static List<AccountEntryType> getAccountActivityLastXDays(final int days) {
		final List<AccountEntryType> accountEntries = new ArrayList<>();
		int daysLeft = days;
		final ApiContext api = EbayApiContextManager.getLiveContext();
		while(daysLeft > 0) {
			try {
				Calendar from = Calendar.getInstance();
				from.add(Calendar.DAY_OF_YEAR, daysLeft * -1);
				Calendar to = Calendar.getInstance();
				if(daysLeft > 29) {
					to.add(Calendar.DAY_OF_YEAR, (daysLeft * -1) + 29);
				}
				TimeFilter filter = new TimeFilter(from, to);
				final GetAccountCall call = new GetAccountCall(api);
				call.setAccountEntrySortType(AccountEntrySortTypeCodeType.ACCOUNT_ENTRY_CREATED_TIME_DESCENDING);
				call.setViewType(AccountHistorySelectionCodeType.BETWEEN_SPECIFIED_DATES);
				call.setViewPeriod(filter);
				final AccountEntryType[] entries = call.getAccount();
				for(final AccountEntryType entry : entries) {
					accountEntries.add(entry);
				}
				daysLeft -= 29;
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
		
		return accountEntries;
	}
	
	public static void checkAPIAccessRules() {
		try { 
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final GetApiAccessRulesCall call = new GetApiAccessRulesCall(api);
			call.getApiAccessRules();
			final ApiAccessRuleType[] accessRules = call.getReturnedApiAccessRules();
			for(final ApiAccessRuleType rule : accessRules) {
				System.out.println("Rule: " + rule.getCallName());
				System.out.println("\tDaily Hard Limit: " + rule.getDailyHardLimit());
				System.out.println("\tDaily Usage: " + rule.getDailyUsage());
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean setShipmentTrackingInfo(final ProcessedOrder order, final TrackingEntry entry) {
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final CompleteSaleCall call = new CompleteSaleCall(api);
			final Optional<CustomerOrder> customerOrder = CustomerOrderManager.loadCustomerOrderById(order.customer_order_id);
			if(customerOrder.isPresent()) {
				call.setTransactionID(customerOrder.get().marketplace_order_id);
				final MarketplaceListing marketListing = MarketplaceLoader.loadMarketplaceListingById(customerOrder.get().marketplace_listing_id);
				if(marketListing != null) {
					call.setItemID(marketListing.listingId);
					call.setShipped(true);
					final ShipmentType shipmentDetails = new ShipmentType();
					shipmentDetails.setDeliveryStatus(entry.shipmentStatus);
					System.out.println("Shipment status: " + entry.shipmentStatus);
					final ShipmentTrackingDetailsType trackingDetails = new ShipmentTrackingDetailsType();
					trackingDetails.setShipmentTrackingNumber(entry.trackingNumber);
					System.out.println("Shipping service: " + entry.shippingService);
					trackingDetails.setShippingCarrierUsed(entry.shippingService);
					shipmentDetails.setShipmentTrackingDetails(new ShipmentTrackingDetailsType[]{trackingDetails});
					call.setShipment(shipmentDetails);
					System.out.println("Updating tracking details on eBay for order " + order.id);
					call.completeSale();
					return !call.hasError();
				}
			}
		} catch(final Exception e) {
			if(entry != null) {
				DBLogging.high(EbayCalls.class, "failed to set shipment tracking info for order " + order + " and tracking entry " + entry + ": ", e);
			}
		}

		return false;
	}

	public static EbayCategory[] getSuggestedCategories(final String listingTitle) {
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final GetSuggestedCategoriesCall call = new GetSuggestedCategoriesCall(api);
		call.setQuery(listingTitle);
		try {
			return Arrays.stream(call.getSuggestedCategories())
					.map(category -> new EbayCategory(category.getCategory().getCategoryID(), category.getCategory().getCategoryName()))
					.toArray(EbayCategory[]::new);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return new EbayCategory[0];
	}

	public static Optional<String> createListing(final Listing listing) {
		final ApiContext api = EbayApiContextManager.getLiveContext();
		final AddFixedPriceItemCall call = new AddFixedPriceItemCall(api);
		call.setItem(createItemTypeForListing(call, listing));
		try {
			call.addFixedPriceItem();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return Optional.ofNullable(call.getReturnedItemID());
	}

	private static ItemType createItemTypeForListing(final AddFixedPriceItemCall call, final Listing listing) {
		final ItemType item = new ItemType();
		item.setBuyerRequirementDetails(createBuyerRequirementsForListing(listing));
		item.setCountry(CountryCodeType.US);
		item.setCurrency(CurrencyCodeType.USD);
		item.setDescription(listing.description);
		item.setDispatchTimeMax(5);
		listing.handlingTime = 5;
		item.setListingDuration(ListingDurationCodeType.GTC.value());
		item.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);
		item.setLocation("St. Louis, MO");
		item.setPostalCode("63101");
		item.setPayPalEmailAddress("thevikingmarketplace@gmail.com");
		item.setProductListingDetails(createProductListingDetailsForListing(listing, listing.upc, listing.ean));
		item.setPictureDetails(createPictureDetailsForListing(listing));
		item.setPrimaryCategory(createCategoryTypeForListing(listing));
		item.setQuantity(0);
		item.setReturnPolicy(createReturnPolicyTypeForListing(listing));
		item.setShippingDetails(createShippingDetailsForListing(listing));
		item.setSite(SiteCodeType.US);
		item.setTitle(listing.title);
		item.setConditionID(1000); //brand new

		final NameValueListArrayType specifics = new NameValueListArrayType();
		final List<NameValueListType> itemSpecifics = new ArrayList<>();//Arrays.asList(brand, upcOrEan, mpn));
		
		final NameValueListType brand = new NameValueListType();
		brand.setName("Brand");
		brand.setValue(new String[]{listing.brand});
		
		if(listing.ean != null) {
			final NameValueListType ean = new NameValueListType();
			ean.setName("EAN");
			ean.setValue(new String[]{listing.ean});
			itemSpecifics.add(ean);
		}
		
		if(listing.itemSpecifics != null) {
			for(final Map.Entry<String, String> providedItemSpecific : listing.itemSpecifics.entrySet()) {
				if(providedItemSpecific.getValue() != null) {
					final NameValueListType specific = new NameValueListType();
					specific.setName(providedItemSpecific.getKey());
					specific.setValue(new String[] {providedItemSpecific.getValue()});
					itemSpecifics.add(specific);
				}
			}
		}
		
		itemSpecifics.add(brand);

		specifics.setNameValueList(itemSpecifics.toArray(new NameValueListType[itemSpecifics.size()]));
		item.setItemSpecifics(specifics);

		final BuyerPaymentMethodCodeType[] paymentMethods = {BuyerPaymentMethodCodeType.PAY_PAL};
		item.setPaymentMethods(paymentMethods);


		final AmountType price = new AmountType();
		price.setCurrencyID(CurrencyCodeType.USD);
		price.setValue(listing.price);
		item.setStartPrice(price);
		return item;
	}

	private static BuyerRequirementDetailsType createBuyerRequirementsForListing(final Listing listing) {
		final BuyerRequirementDetailsType type = new BuyerRequirementDetailsType();
		type.setShipToRegistrationCountry(true);
		return type;
	}
	
	private static ProductListingDetailsType createProductListingDetailsForListing(final Listing listing, final String upc, final String ean) {
		final ProductListingDetailsType type = new ProductListingDetailsType();
		if(upc != null) {
			type.setUPC(upc);
		} else if(ean != null) {
			type.setEAN(ean);
		} else {
			type.setUPC("Does Not Apply");
		}
		final BrandMPNType brandMpn = new BrandMPNType();
		System.out.println("Setting brand: " + listing.brand + " and mpn: " + listing.mpn);
		brandMpn.setBrand(listing.brand == null ? "Unbranded" : listing.brand);
		brandMpn.setMPN(listing.mpn != null ? listing.mpn : "Does Not Apply");
		type.setBrandMPN(brandMpn);
		type.setIncludeeBayProductDetails(true);
		type.setIncludeStockPhotoURL(false);
		type.setUseStockPhotoURLAsGallery(false);
		type.setUseFirstProduct(true);
		return type;
	}

	private static PictureDetailsType createPictureDetailsForListing(final Listing listing) {
		final PictureDetailsType type = new PictureDetailsType();
		final String[] urls = listing.pictures.stream()
				.map(image -> image.url.replace("http://", "https://")) //eBay requires external images to have a link with https
				.toArray(String[]::new);

		type.setExternalPictureURL(urls);
		return type;
	}

	private static CategoryType createCategoryTypeForListing(final Listing listing) {
		final CategoryType type = new CategoryType();
		type.setCategoryID(listing.category.id);
		return type;
	}

	private static ReturnPolicyType createReturnPolicyTypeForListing(final Listing listing) {
		final ReturnPolicyType type = new ReturnPolicyType();
		type.setReturnsAcceptedOption(ReturnsAcceptedCodeType.RETURNS_NOT_ACCEPTED.value());
		type.setInternationalReturnsAcceptedOption(ReturnsAcceptedCodeType.RETURNS_NOT_ACCEPTED.value());
		return type;
	}

	private static ShippingDetailsType createShippingDetailsForListing(final Listing listing) {
		final ShippingDetailsType type = new ShippingDetailsType();
		type.setGlobalShipping(false);
		type.setShippingServiceOptions(createShippingServiceOptionsForListing(listing));

		return type;
	}

	private static ShippingServiceOptionsType[] createShippingServiceOptionsForListing(final Listing listing) {
		final ShippingServiceOptionsType type = new ShippingServiceOptionsType();
		type.setFreeShipping(listing.shipping <= 0);
		type.setShippingService(listing.shippingService.value());

		final AmountType shippingCost = new AmountType();
		shippingCost.setCurrencyID(CurrencyCodeType.USD);
		shippingCost.setValue(listing.shipping);
		type.setShippingServiceCost(shippingCost);

		return new ShippingServiceOptionsType[] {type};
	}
	
	public static Map<String, List<String>> getRequiredItemSpecificFields(final String categoryId) {
		final Map<String, List<String>> requiredFields = new HashMap<>();
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final GetCategorySpecificsCall call = new GetCategorySpecificsCall(api);
			call.setCategoryID(new String[] {categoryId});
			final RecommendationsType[] recommendations = call.getCategorySpecifics();
			if(recommendations != null) {
				for(final RecommendationsType recommendation : recommendations) {
					final NameRecommendationType[] specificRecommendations = recommendation.getNameRecommendation();
					for(final NameRecommendationType specificRecommendation : specificRecommendations) {
						final RecommendationValidationRulesType validationRules = specificRecommendation.getValidationRules();
						if(validationRules != null && validationRules.getMinValues() != null && validationRules.getMinValues() > 0) {
							final List<String> recommendedValues = Arrays.stream(specificRecommendation.getValueRecommendation())
									.map(rec -> rec.getValue())
									.collect(Collectors.toList());
							
							requiredFields.put(specificRecommendation.getName(), recommendedValues);
						}
					}
				}
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Required item specific fields: " + requiredFields);
		return requiredFields;
	}
}
