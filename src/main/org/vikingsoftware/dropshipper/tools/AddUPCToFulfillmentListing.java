package main.org.vikingsoftware.dropshipper.tools;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsProductAPI;

public final class AddUPCToFulfillmentListing {
	
	private AddUPCToFulfillmentListing() {
		//util class
	}
	
	public static void main(final String[] args) {
		final List<FulfillmentListing> listings = FulfillmentManager.get().getListingsForFulfillmentPlatform(FulfillmentPlatforms.SAMS_CLUB);
		final SamsProductAPI api = new SamsProductAPI();
		listings.removeIf(listing -> listing.upc != null || listing.ean != null);
		System.out.println("Loaded " + listings.size() + " Sams Club listings");
		
		for(final FulfillmentListing listing : listings) {
			if(api.parse(listing.product_id)) {
				final Optional<String> upc = api.getUPC();
				if(upc.isPresent()) {
					System.out.println("upc: " + upc.get());
					updateDB(listing.id, "upc", upc.get());
				} else {
					final Optional<String> ean = api.getEAN();
					if(ean.isPresent()) {
						System.out.println("ean: " + ean.get());
						updateDB(listing.id, "ean", ean.get());
					} else {
						System.err.println("Failed to parse UPC or EAN for listing w/ product id: " + listing.product_id);
					}
				}
			} else {
				System.err.println("Failed to parse api response for listing w/ product id: " + listing.product_id);
				//System.exit(0);
			}
		}
	}
	
	private static void updateDB(final int fulfillmentListingId, final String column, final String value) {
		try(final Statement st = VSDSDBManager.get().createStatement()) {
			System.out.println(value + ": " + value);
			st.execute("UPDATE fulfillment_listing SET " + column + "="+value+" WHERE id="+fulfillmentListingId);
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
