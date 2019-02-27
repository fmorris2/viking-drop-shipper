package main.org.vikingsoftware.dropshipper.core.data.sku;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

public class SkuMappingManager {
	
	private static Map<SkuMappingKey, SkuMapping> mappings = new HashMap<>();
	
	public static boolean load() {
		mappings.clear();
		final Statement st = VDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM sku_mappings");
			while(results.next()) {
				final SkuMapping mapping = new SkuMapping.Builder()
					.id(results.getInt("id"))
					.marketplace_listing_id(results.getInt("marketplace_listing_id"))
					.item_sku(results.getString("item_sku"))
					.ali_express_options(results.getString("ali_express_options"))
					.build();
				
				mappings.put(new SkuMappingKey(mapping.marketplace_listing_id, mapping.item_sku), mapping);
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		
		return !mappings.isEmpty();
	}
	
	public static Optional<SkuMapping> getMapping(final int marketplaceListingId, final String sku) {
		if(mappings.isEmpty()) {
			load();
		}
		
		return Optional.ofNullable(mappings.get(new SkuMappingKey(marketplaceListingId, sku)));
	}
	
	private static class SkuMappingKey {
		private final int marketplaceListingId;
		private final String itemSku;
		
		public SkuMappingKey(final int id, final String sku) {
			this.marketplaceListingId = id;
			this.itemSku = sku;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((itemSku == null) ? 0 : itemSku.hashCode());
			result = prime * result + marketplaceListingId;
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final SkuMappingKey other = (SkuMappingKey) obj;
			if (itemSku == null) {
				if (other.itemSku != null)
					return false;
			} else if (!itemSku.equals(other.itemSku))
				return false;
			if (marketplaceListingId != other.marketplaceListingId)
				return false;
			return true;
		}
	}
}
