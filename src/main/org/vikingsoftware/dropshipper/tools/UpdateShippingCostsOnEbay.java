package main.org.vikingsoftware.dropshipper.tools;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.call.ReviseFixedPriceItemCall;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public class UpdateShippingCostsOnEbay {

	public static void main(final String[] args) {
		final ExecutorService executor = Executors.newFixedThreadPool(4);
		final ApiContext ebayApi = EbayApiContextManager.getLiveContext();
		try (final Statement st = VSDSDBManager.get().createStatement();
				final ResultSet res = st.executeQuery("SELECT listing_id,current_shipping_cost FROM marketplace_listing WHERE"
								+ " current_shipping_cost > 0 AND is_purged=0")) {
			while(res.next()) {
				String marketListingId = res.getString("listing_id");
				executor.execute(() -> {
					try {
						final ItemType item = new ItemType();
						item.setItemID(marketListingId);
						item.setQuantity(0);
	
						final ReviseFixedPriceItemCall call = new ReviseFixedPriceItemCall(ebayApi);
	
						final Listing template = new Listing();
						template.shippingService = ShippingServiceCodeType.SHIPPING_METHOD_STANDARD;
						template.shipping = 0.0;
	
						item.setShippingDetails(EbayCalls.createShippingDetailsForListing(template));
						call.setItemToBeRevised(item);
	
						call.reviseFixedPriceItem();
						System.out.println("Successfully updated shipping details for listing https://www.ebay.com/itm/"
								+ marketListingId);
					} catch(final Exception e) {
						System.err.println("Failed to update shipping details for listing https://www.ebay.com/itm/" + marketListingId);
					}
				});
			}
			executor.shutdown();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
