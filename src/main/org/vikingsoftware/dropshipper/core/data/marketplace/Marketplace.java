package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class Marketplace {

	public final int id;
	public final String marketplace_name;
	public final URL marketplace_url;
	public final double marketplace_profit_cut;

	private final Set<String> knownOrderIds;
	private final Map<String, Integer> listings;

	private Marketplace(final Builder builder) {
		this.id = builder.id;
		this.marketplace_name = builder.marketplace_name;
		this.marketplace_url = builder.marketplace_url;
		this.marketplace_profit_cut = builder.marketplace_profit_cut;
		this.knownOrderIds = builder.knownOrderIds;
		this.listings = builder.listings;
	}

	public boolean isOrderIdKnown(final String id) {
		return knownOrderIds.contains(id);
	}

	public void addKnownOrderId(final String id) {
		knownOrderIds.add(id);
	}

	public void clearKnownOrderIds() {
		knownOrderIds.clear();
	}

	public Set<MarketplaceListing> getMarketplaceListings() {
		return getMarketplaceListings(false);
	}

	public Set<MarketplaceListing> getActiveMarketplaceListings() {
		return getMarketplaceListings(true);
	}

	public int getMarketplaceListingIndex(final String listingId) {
		return listings.getOrDefault(listingId, -1);
	}

	private Set<MarketplaceListing> getMarketplaceListings(final boolean activeOnly) {
		final Set<MarketplaceListing> listings = new HashSet<>();
		
		String sql = "SELECT * FROM marketplace_listing INNER JOIN fulfillment_mapping ON marketplace_listing.id=fulfillment_mapping.marketplace_listing_id "
				+ "WHERE marketplace_id="+id;
		if(activeOnly) {
			sql += " AND active=1";
		}
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet results = st.executeQuery(sql)) {
			while(results.next()) {
				final MarketplaceListing listing = Marketplace.loadListingFromResultSet(results);
				listings.add(listing);
			}
		} catch (final SQLException e) {
			DBLogging.high(getClass(), "failed to getMarketplaceListings: ", e);
		}

		return listings;
	}

	public static MarketplaceListing loadListingFromResultSet(final ResultSet results) throws SQLException {
		return new MarketplaceListing.Builder()
			.id(results.getInt("id"))
			.marketplaceId(results.getInt("marketplace_id"))
			.listingId(results.getString("listing_id"))
			.listingTitle(results.getString("listing_title"))
			.active(results.getBoolean("active"))
			.currentEbayInventory(results.getInt("current_ebay_inventory"))
			.currentPrice(results.getDouble("current_price"))
			.currentShippingCost(results.getDouble("current_shipping_cost"))
			.targetMargin(results.getDouble("target_margin"))
			.fulfillmentQuantityMultiplier(results.getInt("fulfillment_quantity_multiplier"))
			.current_handling_time(results.getInt("current_handling_time"))
			.target_handling_time(results.getInt("target_handling_time"))
		.build();
	}

	public static class Builder {
		private int id;
		private String marketplace_name;
		private URL marketplace_url;
		private double marketplace_profit_cut;
		private Set<String> knownOrderIds;
		private Map<String, Integer> listings;

		public Builder id(final int id) {
			this.id = id;
			return this;
		}

		public Builder marketplace_name(final String name) {
			this.marketplace_name = name;
			return this;
		}

		public Builder marketplace_url(final String url) {
			try {
				this.marketplace_url = new URL(url);
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
			return this;
		}

		public Builder marketplace_profit_cut(final double cut) {
			this.marketplace_profit_cut = cut;
			return this;
		}

		public Builder known_order_ids(final Set<String> knownOrderIds) {
			this.knownOrderIds = knownOrderIds;
			return this;
		}

		public Builder listings(final Map<String, Integer> listings) {
			this.listings = listings;
			return this;
		}

		public Marketplace build() {
			return new Marketplace(this);
		}
	}
}
