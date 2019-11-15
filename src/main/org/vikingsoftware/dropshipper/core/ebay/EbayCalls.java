package main.org.vikingsoftware.dropshipper.core.ebay;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.AddFixedPriceItemCall;
import com.ebay.sdk.call.CompleteSaleCall;
import com.ebay.sdk.call.GetApiAccessRulesCall;
import com.ebay.sdk.call.GetItemCall;
import com.ebay.sdk.call.GetItemTransactionsCall;
import com.ebay.sdk.call.GetOrdersCall;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.sdk.call.GetSuggestedCategoriesCall;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.sdk.call.ReviseInventoryStatusCall;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.ApiAccessRuleType;
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
import com.ebay.soap.eBLBaseComponents.NameValueListArrayType;
import com.ebay.soap.eBLBaseComponents.NameValueListType;
import com.ebay.soap.eBLBaseComponents.OrderIDArrayType;
import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.PaginationType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import com.ebay.soap.eBLBaseComponents.ProductListingDetailsType;
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
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
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
	private static final int MIN_AVAILABLE_FULFILLMENT_QTY = 75;

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

	public static CustomerOrder[] getOrdersLastXDays(final int days) {
		try {
			final ApiContext apiContext = EbayApiContextManager.getLiveContext();
			final GetSellerTransactionsCall call = new GetSellerTransactionsCall(apiContext);
			call.setIncludeFinalValueFee(true);
			call.setDetailLevel(new DetailLevelCodeType[]{DetailLevelCodeType.RETURN_ALL});
			final PaginationType pagination = new PaginationType();
			pagination.setEntriesPerPage(200);
			pagination.setPageNumber(1);
			call.setPagination(pagination);
			final Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_YEAR, days * -1);
			call.setTimeFilter(new TimeFilter(cal, Calendar.getInstance())); //will use number of days filter
			final TransactionType[] transactions = call.getSellerTransactions();
			
			if(transactions == null) {
				return new CustomerOrder[0];
			}
			
			final List<CustomerOrder> orders = new ArrayList<>();
			final List<TransactionType> unknownTransactionMappings = new ArrayList<>();
			
			System.out.println("Num eBay transactions in last " + days + " days: " + transactions.length);
			for(final TransactionType trans : transactions) {
				final CustomerOrder order = EbayConversionUtils.convertTransactionTypeToCustomerOrder(trans.getItem().getItemID(), trans);
				if(order != null) {
					orders.add(order);
				} else {
					unknownTransactionMappings.add(trans);
				}
			}

			logUnknownTransactionMappingsInDB(unknownTransactionMappings);
			return orders.toArray(new CustomerOrder[orders.size()]);

		} catch(final Exception e) {
			DBLogging.high(EbayCalls.class, "failed to get orders last " + days + " days: ", e);
		}

		return new CustomerOrder[0];
	}

	private static void logUnknownTransactionMappingsInDB(final List<TransactionType> transactions) {
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			System.out.println("Logging " + transactions.size() + " unknown eBay transactions in DB");
			for(final TransactionType trans : transactions) {
				st.addBatch("INSERT INTO unknown_transaction_mappings(marketplace_id, listing_id) "
						+ "VALUES("+Marketplaces.EBAY.getMarketplaceId()+", '"+trans.getItem().getItemID()+"')");
			}
			st.executeBatch();
		} catch(final Exception e) {
			DBLogging.high(EbayCalls.class, "Failed to insert unknown transaction mappings into DB", e);
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
		} catch(final Exception e) {
			e.printStackTrace();
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
			if(inventory < MIN_AVAILABLE_FULFILLMENT_QTY) {
				invStatus.setQuantity(0);
			} else {
				invStatus.setQuantity(Math.max(0, Math.min(FAKE_MAX_QUANTITY, inventory)));
			}
			System.out.println("Setting stock for listing id " + listingId + " to " + invStatus.getQuantity());
			call.setInventoryStatus(new InventoryStatusType[] {invStatus});
			call.reviseInventoryStatus();
			logToFile("updateInventory: listingId - " + listingId + ", quantity: " + Math.max(0, Math.min(FAKE_MAX_QUANTITY, invStatus.getQuantity())));
			return !call.hasError();
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.medium(EbayCalls.class, "failed to update inventory for listing " + listingId + " w/ stock " + inventory+ ": ", e);
		
			if(e.getMessage().equals("You are not allowed to revise ended listings.")) {
				System.out.println("setting ended listing to inactive in database...");
				if(MarketplaceListing.setIsActive(listingId, false)) {
					System.out.println("\tsuccess");
				} else {
					System.out.println("\tfailure");
				}
			}
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
//		checkAPIAccessRules();
//		MarketplaceLoader.loadMarketplaces();
//		final Set<MarketplaceListing> listings = Marketplaces.EBAY.getMarketplace().getMarketplaceListings();
//		for(final MarketplaceListing listing : listings) {
//			updateHandlingTime(listing.listingId, 4);
//		}
		
		getOrdersLastXDays(2);
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
		call.setItem(createItemTypeForListing(listing));
		try {
			call.addFixedPriceItem();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return Optional.ofNullable(call.getReturnedItemID());
	}

	private static ItemType createItemTypeForListing(final Listing listing) {
		final ItemType item = new ItemType();
		item.setBuyerRequirementDetails(createBuyerRequirementsForListing(listing));
		item.setCountry(CountryCodeType.US);
		item.setCurrency(CurrencyCodeType.USD);
		item.setDescription(listing.description);
		item.setDispatchTimeMax(4);
		item.setListingDuration(ListingDurationCodeType.GTC.value());
		item.setListingType(ListingTypeCodeType.FIXED_PRICE_ITEM);
		item.setLocation("St. Louis, MO");
		item.setPostalCode("63101");
		item.setPayPalEmailAddress("thevikingmarketplace@gmail.com");
		item.setProductListingDetails(createProductListingDetailsForListing(listing));
		item.setPictureDetails(createPictureDetailsForListing(listing));
		item.setPrimaryCategory(createCategoryTypeForListing(listing));
		item.setQuantity(0);
		item.setReturnPolicy(createReturnPolicyTypeForListing(listing));
		item.setShippingDetails(createShippingDetailsForListing(listing));
		item.setSite(SiteCodeType.US);
		item.setTitle(listing.title);
		item.setConditionID(1000); //brand new

		final NameValueListArrayType specifics = new NameValueListArrayType();

		final NameValueListType brand = new NameValueListType();
		brand.setName("Brand");
		brand.setValue(new String[]{listing.brand});

		final NameValueListType upc = new NameValueListType();
		upc.setName("UPC");
		upc.setValue(new String[]{"Does Not Apply"});

		final NameValueListType[] specificsVals = {brand, upc};
		specifics.setNameValueList(specificsVals);
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
	
	private static ProductListingDetailsType createProductListingDetailsForListing(final Listing listing) {
		final ProductListingDetailsType type = new ProductListingDetailsType();
		type.setUPC("Does not apply");
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
}
