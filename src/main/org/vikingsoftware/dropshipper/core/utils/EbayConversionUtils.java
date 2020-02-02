package main.org.vikingsoftware.dropshipper.core.utils;

import com.ebay.soap.eBLBaseComponents.AddressType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.PaymentTransactionType;
import com.ebay.soap.eBLBaseComponents.PaymentsInformationType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;

public class EbayConversionUtils {
	private EbayConversionUtils(){}

	public static CustomerOrder convertTransactionTypeToCustomerOrder(final int dbListingId, final String transactionId, 
			final TransactionType transaction) {
		
		try {
			System.out.println("EbayConversionUtils#convertTransactionTypeToCustomerOrder START");
			final UserType buyer = transaction.getBuyer();
			final AddressType addr = buyer.getBuyerInfo().getShippingAddress();
			final ItemType item = transaction.getItem();
			
			final PaymentsInformationType monetaryDetails = transaction.getMonetaryDetails();
			final PaymentTransactionType initialBuyerPayment = monetaryDetails.getPayments().getPayment(0);
			System.out.println("About to call EbayCalls#getHandlingTime");
			//final Integer handlingTime = EbayCalls.getHandlingTime(transactionId).orElse(-1);
			final Integer handlingTime = MarketplaceListing.getCurrentHandlingTime(dbListingId).orElse(-1);
			final Integer fulfillmentQtyMultiplier = MarketplaceListing.getFulfillmentQuantityMultiplier(dbListingId).orElse(-1);
			System.out.println("\tdone. About to build customer order.");
			
			return new CustomerOrder.Builder()
				.marketplace_listing_id(dbListingId)
				.sku(item.getSKU())
				.sell_listing_price(transaction.getTransactionPrice().getValue())
				.sell_shipping(transaction.getActualShippingCost().getValue())
				.sell_total((transaction.getTransactionPrice().getValue() + transaction.getActualShippingCost().getValue()) * transaction.getQuantityPurchased())
				.quantity(transaction.getQuantityPurchased())
				.marketplace_order_id(transaction.getTransactionID())
				.buyer_username(buyer.getUserID())
				.buyer_name(buyer.getUserFirstName() + " " + buyer.getUserLastName())
				.buyer_country(addr.getCountryName())
				.buyer_street_address(addr.getStreet1())
				.buyer_apt_suite_unit_etc(addr.getStreet2())
				.buyer_state_province_region(addr.getStateOrProvince())
				.buyer_city(addr.getCityName())
				.buyer_zip_postal_code(addr.getPostalCode())
				.buyer_phone_number(addr.getPhone())
				.date_parsed(transaction.getPaidTime().toInstant().toEpochMilli())
				.payment_processor_fee_date(initialBuyerPayment.getPaymentTime().toInstant().toEpochMilli())
				.payment_processor_fee(initialBuyerPayment.getFeeOrCreditAmount().getValue() * -1)
				.marketplace_sell_fee(transaction.getFinalValueFee().getValue() * -1)
				.handling_time(handlingTime)
				.snapshot_fulfillment_quantity_multiplier(fulfillmentQtyMultiplier)
				.build();
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
