package main.org.vikingsoftware.dropshipper.core.utils;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;

import com.ebay.soap.eBLBaseComponents.AddressType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;

public class EbayConversionUtils {
	private EbayConversionUtils(){}
	
	public static CustomerOrder convertTransactionTypeToCustomerOrder(final int marketplaceListingId, final TransactionType transaction) {
		if(transaction == null) {
			return null;
		}
		
		final UserType buyer = transaction.getBuyer();
		final AddressType addr = buyer.getBuyerInfo().getShippingAddress();
		final ItemType item = transaction.getItem();
		
		return new CustomerOrder.Builder()
			.marketplace_listing_id(marketplaceListingId)
			.sku(item.getSKU())
			.sale_price(transaction.getAmountPaid().getValue())
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
			.build();
	}
}
