package main.org.vikingsoftware.dropshipper.tools;

import java.sql.PreparedStatement;
import java.util.List;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubProductAPI;

public class AddSkuIdToFulfillmentListing {
	
	private AddSkuIdToFulfillmentListing() {
		//util class
	}
	
	public static void main(final String[] args) {
		final List<FulfillmentListing> listings = FulfillmentManager.get().getListingsForFulfillmentPlatform(FulfillmentPlatforms.SAMS_CLUB);
		final SamsClubProductAPI api = new SamsClubProductAPI();
		listings.removeIf(listing -> listing.sku_id != null);
		System.out.println("Loaded " + listings.size() + " Sams Club listings");
		
		for(final FulfillmentListing listing : listings) {
			if(api.parse(listing.product_id)) {
				api.getSkuId().ifPresent(sku -> updateDB(listing.id, "sku_id", sku));
			} else {
				System.err.println("Failed to parse api response for listing w/ product id: " + listing.product_id);
				//System.exit(0);
			}
		}
	}
	
	private static void updateDB(final int fulfillmentListingId, final String column, final String value) {
		try(final PreparedStatement st = VSDSDBManager.get().createPreparedStatement("UPDATE fulfillment_listing SET " + column + "=? WHERE id=?")) {
			System.out.println("Setting col " + column + " to value" + ": " + value);
			st.setString(1, value);
			st.setInt(2, fulfillmentListingId);
			st.execute();
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
