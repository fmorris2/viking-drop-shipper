package main.org.vikingsoftware.dropshipper.tools;

import java.sql.Statement;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.SamsClubMetaDataParser;

public final class AddProductIdToFulfillmentListing {

	private static final String PRODUCT_ID_REGEX = "\\/(prod)*(\\d{5,})";
	private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile(PRODUCT_ID_REGEX);
	
	private static final SamsClubMetaDataParser parser = new SamsClubMetaDataParser();
	
	private AddProductIdToFulfillmentListing() {
		//empty
	}
	
	public static void main(final String[] args) {
		final List<FulfillmentListing> listings = FulfillmentManager.get().getListingsForFulfillmentPlatform(FulfillmentPlatforms.SAMS_CLUB);
		//listings.removeIf(listing -> listing.product_id != null);
		System.out.println("Loaded " + listings.size() + " fulfillment listings for sams club");
		
		for(final FulfillmentListing listing : listings) {
			boolean success = false;
			try {
				final String pageSource = Jsoup.connect(listing.listing_url).get().html();
				parser.parse(pageSource);
				final String productId = parser.getProductID();
				if(productId != null) {
					insertProductId(listing.id, productId);
					success = true;
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}
			
			if(!success) {
				final Matcher matcher = PRODUCT_ID_PATTERN.matcher(listing.listing_url);
				if(matcher.find()) {
					final String prodPrefix = matcher.group(1);
					insertProductId(listing.id, (prodPrefix == null ? "" : prodPrefix) + matcher.group(2));
				} else {
					System.err.println("Failed to parse product id for listing w/ url: " + listing.listing_url);
					System.exit(0);
				}
			}
		}
	}
	
	private static void insertProductId(final int fulfillmentListingId, final String productId) {
		System.out.println("Inserting product id " + productId + " for fulfillment listing " + fulfillmentListingId);
		try(final Statement st = VSDSDBManager.get().createStatement()) {
			st.execute("UPDATE fulfillment_listing SET product_id='"+productId+"' WHERE id="+fulfillmentListingId);
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
