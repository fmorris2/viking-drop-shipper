package main.org.vikingsoftware.dropshipper.core.ebay;

import java.util.Arrays;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.utils.EbayConversionUtils;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.soap.eBLBaseComponents.TransactionType;

public class EbayCalls {
	
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
			e.printStackTrace();
		}
		
		return new CustomerOrder[0];
	}
}
