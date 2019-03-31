package main.org.vikingsoftware.dropshipper.core.ebay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.CompleteSaleCall;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ShipmentTrackingDetailsType;
import com.ebay.soap.eBLBaseComponents.ShipmentType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.VariationType;
import com.ebay.soap.eBLBaseComponents.VariationsType;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.EbayConversionUtils;

public class EbayCalls {

	private static final int FAKE_MAX_QUANTITY = 2;

	private EbayCalls() {}

	public static CustomerOrder[] getOrdersLastXDays(final int days) {
		try {
			final ApiContext apiContext = EbayApiContextManager.getLiveContext();
			final GetSellerTransactionsCall call = new GetSellerTransactionsCall(apiContext);
			call.setTimeFilter(new TimeFilter(null, null)); //will use number of days filter
			call.setNumberOfDays(days);
			final TransactionType[] transactions = call.getSellerTransactions();
			final String listingId = transactions[0].getItem().getItemID();
			final int dbListingId = Marketplaces.EBAY.getMarketplace().getMarketplaceListingIndex(listingId);
			return Arrays.stream(transactions)
					.map(trans -> EbayConversionUtils.convertTransactionTypeToCustomerOrder(dbListingId, trans))
					.toArray(CustomerOrder[]::new);

		} catch(final Exception e) {
			DBLogging.high(EbayCalls.class, "failed to get orders last " + days + " days: ", e);
		}

		return new CustomerOrder[0];
	}

	public static boolean updateInventory(final String listingId, final List<SkuInventoryEntry> invEntries) {
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final ReviseFixedPriceItemCall call = new ReviseFixedPriceItemCall(api);
			final ItemType itemToRevise = new ItemType();
			itemToRevise.setItemID(listingId);
			if(invEntries.size() == 1 && invEntries.get(0).sku == null) {
				itemToRevise.setQuantity(Math.min(FAKE_MAX_QUANTITY, invEntries.get(0).stock));
			} else {
				final VariationsType variations = new VariationsType();
				final List<VariationType> entries = new ArrayList<>();
				for(final SkuInventoryEntry entry : invEntries) {
					final VariationType variation = new VariationType();
					variation.setSKU(entry.sku);
					variation.setQuantity(Math.min(FAKE_MAX_QUANTITY, entry.stock));
					entries.add(variation);
				}
				variations.setVariation(entries.stream().toArray(VariationType[]::new));
				System.out.println("Variations: " + variations.getVariationLength());
				itemToRevise.setVariations(variations);
			}
			call.setItemToBeRevised(itemToRevise);
			final int fees = call.reviseFixedPriceItem().getFee().length;
			System.out.println("fees: " + fees);
			return fees > 0;
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.high(EbayCalls.class, "failed to update inventory for listing " + listingId + " and invEntries " + invEntries + ": ", e);
		}

		return false;
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
					shipmentDetails.setNotes(entry.deliveryRemark);
					System.out.println("Delivery Remark: " + entry.deliveryRemark);
					final ShipmentTrackingDetailsType trackingDetails = new ShipmentTrackingDetailsType();
					trackingDetails.setShipmentTrackingNumber(entry.trackingNumber);
					trackingDetails.setShippingCarrierUsed(entry.shippingService);
					shipmentDetails.setShipmentTrackingDetails(new ShipmentTrackingDetailsType[]{trackingDetails});
					call.setShipment(shipmentDetails);
					System.out.println("Updating tracking details on eBay for order " + order.id);
					call.completeSale();
					return true;
				}
			}
		} catch(final Exception e) {
			DBLogging.high(EbayCalls.class, "failed to set shipment tracking info for order " + order + " and tracking entry " + entry + ": ", e);
		}

		return false;
	}
}
