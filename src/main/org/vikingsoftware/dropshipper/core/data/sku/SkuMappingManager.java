package main.org.vikingsoftware.dropshipper.core.data.sku;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class SkuMappingManager {

	private static Map<SkuMappingKey, SkuMapping> mappings = new HashMap<>();
	private static Map<Integer, List<SkuMapping>> marketplaceToMappings = new HashMap<>();

	public static boolean load() {
		clear();
		final Statement st = VSDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM sku_mappings");
			while(results.next()) {
				final SkuMapping mapping = new SkuMapping.Builder()
					.id(results.getInt("id"))
					.marketplace_listing_id(results.getInt("marketplace_listing_id"))
					.item_sku(results.getString("item_sku"))
					.options(results.getString("options"))
					.build();

				mappings.put(new SkuMappingKey(mapping.marketplace_listing_id, mapping.item_sku), mapping);

				final List<SkuMapping> marketplaceMappings = marketplaceToMappings
						.getOrDefault(mapping.marketplace_listing_id, new ArrayList<>());

				marketplaceMappings.add(mapping);
				marketplaceToMappings.put(mapping.marketplace_listing_id, marketplaceMappings);
			}
		} catch (final SQLException e) {
			DBLogging.high(SkuMappingManager.class, "failed to load sku mappings: ", e);
		}

		return !mappings.isEmpty();
	}

	public static void clear() {
		mappings.clear();
		marketplaceToMappings.clear();
	}

	public static Optional<SkuMapping> getMapping(final int marketplaceListingId, final String sku) {
		if(mappings.isEmpty()) {
			load();
		}

		return Optional.ofNullable(mappings.get(new SkuMappingKey(marketplaceListingId, sku)));
	}

	public static List<SkuMapping> getMappingsForMarketplaceListing(final int marketplaceListingId) {
		return marketplaceToMappings.getOrDefault(marketplaceListingId, new ArrayList<>());
	}
}
