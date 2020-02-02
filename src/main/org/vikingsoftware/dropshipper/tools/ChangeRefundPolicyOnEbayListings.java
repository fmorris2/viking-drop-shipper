package main.org.vikingsoftware.dropshipper.tools;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ReturnPolicyType;
import com.ebay.soap.eBLBaseComponents.ReturnsAcceptedCodeType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;

public class ChangeRefundPolicyOnEbayListings {

	public static void main(final String[] args) {
		final ExecutorService executor = Executors.newFixedThreadPool(8);
		final List<FulfillmentListing> listings = FulfillmentManager.get().getListingsForFulfillmentPlatform(FulfillmentPlatforms.SAMS_CLUB);
		final ApiContext ebayApi = EbayApiContextManager.getLiveContext();
		Collections.shuffle(listings);
		for(final FulfillmentListing listing : listings) {
			executor.execute(() -> {
				String marketListingId = null;
				try {
					final FulfillmentListing list = listing;
					final MarketplaceListing marketListing = MarketplaceListing.getMarketplaceListingForFulfillmentListing(list.id);
					if (marketListing != null) {

						marketListingId = marketListing.listingId;

						final ReviseFixedPriceItemCall call = new ReviseFixedPriceItemCall(ebayApi);
						final ItemType itemToRevise = new ItemType();
						itemToRevise.setItemID(marketListing.listingId);
						final ReturnPolicyType returnPolicyType = new ReturnPolicyType();
						returnPolicyType.setReturnsAcceptedOption(ReturnsAcceptedCodeType.RETURNS_ACCEPTED.value());
						returnPolicyType.setInternationalReturnsAcceptedOption(
								ReturnsAcceptedCodeType.RETURNS_NOT_ACCEPTED.value());
						returnPolicyType.setReturnsWithin("Days_30");
						itemToRevise.setDispatchTimeMax(2);
						itemToRevise.setReturnPolicy(returnPolicyType);

						call.setItemToBeRevised(itemToRevise);
						call.reviseFixedPriceItem();
						System.out.println("Successfully updated return policy for listing https://www.ebay.com/itm/"
								+ marketListingId);
					}
				} catch(final Exception e) {
					System.err.println("Failed to update info for listing https://www.ebay.com/itm/" + marketListingId);
					e.printStackTrace();			
				}
			});
		}
	}
	
}
