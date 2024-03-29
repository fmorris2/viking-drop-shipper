package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class MarketplaceLoader {

	public static void loadMarketplaces() {
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet results = st.executeQuery("SELECT * FROM marketplace")) {
			while(results.next()) {

				final int id = results.getInt("id");

				final Set<String> knownOrderIds = loadKnownOrderIds(id);
				final Map<String, Integer> marketplaceListings = loadMarketplaceListings(id);

				final Marketplace marketplace = new Marketplace.Builder()
					.id(id)
					.marketplace_name(results.getString("marketplace_name"))
					.marketplace_url(results.getString("marketplace_url"))
					.marketplace_profit_cut(results.getDouble("marketplace_profit_cut"))
					.known_order_ids(knownOrderIds)
					.listings(marketplaceListings)
					.build();

				Marketplaces.addMarketplace(marketplace);
			}
		} catch (final SQLException e) {
			DBLogging.high(MarketplaceLoader.class, "failed to load marketplaces: ", e);
		}
	}

	private static Map<String, Integer> loadMarketplaceListings(final int marketplaceId) {
		final Map<String, Integer> listings = new HashMap<>();
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet results = st.executeQuery("SELECT id, listing_id"
					+ " FROM marketplace_listing"
					+ " WHERE marketplace_id=" + marketplaceId)) {

			while(results.next()) {
				final int id = results.getInt("id");
				final String listingId = results.getString("listing_id");
				listings.put(listingId, id);
			}
		} catch (final SQLException e) {
			DBLogging.high(MarketplaceLoader.class, "failed to load marketplace listings for marketplace " + marketplaceId + ": ", e);
		}

		return listings;
	}

	public static MarketplaceListing loadMarketplaceListingById(final int listingId) {
		return loadMarketplaceListing("id", Integer.toString(listingId));
	}

	public static MarketplaceListing loadMarketplaceListingByListingId(final String listingId) {
		return loadMarketplaceListing("listing_id", listingId);
	}

	private static MarketplaceListing loadMarketplaceListing(final String key, final String val) {
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet result = st.executeQuery("SELECT * from marketplace_listing WHERE " + key +"="+val)) {
			if(result.next()) {
				return Marketplace.loadListingFromResultSet(result);
			}
		} catch(final SQLException e) {
			DBLogging.high(MarketplaceLoader.class, "failed to load marketplace listing by " + key + ": " + val, e);
		}

		return null;
	}

	private static Set<String> loadKnownOrderIds(final int marketplaceId) {
		final Set<String> knownOrderIds = new HashSet<>();
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet results = st.executeQuery("SELECT marketplace_order_id "
					+ "FROM customer_order INNER JOIN marketplace_listing ON "
					+ "customer_order.marketplace_listing_id=marketplace_listing.id "
					+ "WHERE marketplace_id=" + marketplaceId)) {

			while(results.next()) {
				final String id = results.getString("marketplace_order_id");
				knownOrderIds.add(id);
			}
		} catch (final SQLException e) {
			DBLogging.high(MarketplaceLoader.class, "failed to load known order ids for marketplace " + marketplaceId + ": ", e);
		}

		return knownOrderIds;
	}
}
