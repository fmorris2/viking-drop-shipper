package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

public class Marketplace {
	
	public final int id;
	public final String marketplace_name;
	public final URL marketplace_url;
	public final double marketplace_profit_cut;
	
	private final Set<String> knownOrderIds;
	
	private Marketplace(final Builder builder) {
		this.id = builder.id;
		this.marketplace_name = builder.marketplace_name;
		this.marketplace_url = builder.marketplace_url;
		this.marketplace_profit_cut = builder.marketplace_profit_cut;
		this.knownOrderIds = builder.knownOrderIds;
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
		final Set<MarketplaceListing> listings = new HashSet<>();
		final Statement st = VDSDBManager.get().createStatement();
		
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM marketplace_listing WHERE marketplace_id="+id);
			while(results.next()) {
				final MarketplaceListing listing = new MarketplaceListing.Builder()
					.id(results.getInt("id"))
					.marketplaceId(id)
					.listingId(results.getString("listing_id"))
					.listingTitle(results.getString("listing_title"))
					.listingUrl(results.getString("listing_url"))
					.listingPrice(results.getDouble("listing_price"))
					.build();
				
				listings.add(listing);
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		
		return listings;
	}
	
	public static class Builder {
		private int id;
		private String marketplace_name;
		private URL marketplace_url;
		private double marketplace_profit_cut;
		private Set<String> knownOrderIds;
		
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
		
		public Marketplace build() {
			return new Marketplace(this);
		}
	}
}
