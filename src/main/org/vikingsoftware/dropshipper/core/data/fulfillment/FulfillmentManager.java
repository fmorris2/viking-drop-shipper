package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

public class FulfillmentManager {
	
	//marketplace listing id ==> fulfillment listing
	private static Map<Integer, List<FulfillmentListing>> listings = new HashMap<>();
	
	//db primary key id ==> fulfillment platform
	private static Map<Integer, FulfillmentPlatform> platforms = new HashMap<>();
	
	public static void load() {
		listings.clear();
		platforms.clear();
		loadFulfillmentListings();
		loadFulfillmentPlatforms();
	}
	
	public static boolean isLoaded() {
		return !listings.isEmpty() && !platforms.isEmpty();
	}
	
	public static List<FulfillmentListing> getListingsForOrder(final CustomerOrder order) {
		return listings.getOrDefault(order.marketplace_listing_id, new ArrayList<>());
	}
	
	private static void loadFulfillmentListings() {
		final Statement st = VDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_listing"
					+ " INNER JOIN fulfillment_mapping ON"
					+ " fulfillment_listing.id=fulfillment_mapping.fulfillment_listing_id");
			while(results.next()) {
				final int marketplace_listing_id = results.getInt("marketplace_listing_id");
				final FulfillmentListing listing = new FulfillmentListing.Builder()
					.id(results.getInt("fulfillment_listing.id"))
					.fulfillment_platform_id(results.getInt("fulfillment_platform_id"))
					.listing_id(results.getString("listing_id"))
					.listing_url(results.getString("listing_url"))
					.listing_max_price(results.getDouble("listing_max_price"))
					.build();
				
				final List<FulfillmentListing> currentListings = listings.getOrDefault(marketplace_listing_id, new ArrayList<>());
				currentListings.add(listing);
				listings.put(marketplace_listing_id, currentListings);
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadFulfillmentPlatforms() {
		final Statement st = VDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM fulfillment_platform");
			while(results.next()) {
				final FulfillmentPlatform platform = new FulfillmentPlatform.Builder()
					.id(results.getInt("id"))
					.platform_name(results.getString("platform_name"))
					.platform_url(results.getString("platform_url"))					
					.build();
				
				platforms.put(platform.id, platform);
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
	}
}
