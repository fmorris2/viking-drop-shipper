package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.Arrays;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;

import org.json.JSONException;
import org.json.JSONObject;

import com.ebay.soap.eBLBaseComponents.AddressType;
import com.ebay.soap.eBLBaseComponents.NameValueListArrayType;
import com.ebay.soap.eBLBaseComponents.NameValueListType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.UserType;
import com.ebay.soap.eBLBaseComponents.VariationType;

public class EbayConversionUtils {
	private EbayConversionUtils(){}
	
	public static CustomerOrder convertTransactionTypeToCustomerOrder(final int marketplaceListingId, final TransactionType transaction) {
		if(transaction == null) {
			return null;
		}
		
		final UserType buyer = transaction.getBuyer();
		final AddressType addr = buyer.getBuyerInfo().getShippingAddress();
		final VariationType variation = transaction.getVariation();
		final NameValueListArrayType varSpecifics = variation.getVariationSpecifics();
		final NameValueListType[] nameValsArr = varSpecifics.getNameValueList();
		final JSONObject json = new JSONObject();
		for(final NameValueListType nameVals : nameValsArr) {
			try {
				json.put(nameVals.getName(), Arrays.asList(nameVals.getValue()));
			} catch (final JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return new CustomerOrder.Builder()
			.marketplace_listing_id(marketplaceListingId)
			.item_options(json.toString())
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
