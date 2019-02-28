package main.org.vikingsoftware.dropshipper.core.ebay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.utils.EbayConversionUtils;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.TransactionType;
import com.ebay.soap.eBLBaseComponents.VariationType;
import com.ebay.soap.eBLBaseComponents.VariationsType;

public class EbayCalls {
	
	private static final int FAKE_MAX_QUANTITY = 50;
	
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
	
	public static boolean updateInventory(final String listingId, final List<SkuInventoryEntry> invEntries) {
		try {
			final ApiContext api = EbayApiContextManager.getLiveContext();
			final ReviseFixedPriceItemCall call = new ReviseFixedPriceItemCall(api);
			final ItemType itemToRevise = new ItemType();
			final VariationsType variations = new VariationsType();
			final List<VariationType> entries = new ArrayList<>();
			for(final SkuInventoryEntry entry : invEntries) {
				final VariationType variation = new VariationType();
				variation.setSKU(entry.sku);
				variation.setQuantity(Math.max(FAKE_MAX_QUANTITY, entry.stock));
				entries.add(variation);
			}
			variations.setVariation(entries.stream().toArray(VariationType[]::new));
			System.out.println("Variations: " + variations.getVariationLength());
			itemToRevise.setItemID(listingId);
			itemToRevise.setVariations(variations);	
			call.setItemToBeRevised(itemToRevise);
			final int fees = call.reviseFixedPriceItem().getFee().length;
			System.out.println("fees: " + fees);
			return fees > 0;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
