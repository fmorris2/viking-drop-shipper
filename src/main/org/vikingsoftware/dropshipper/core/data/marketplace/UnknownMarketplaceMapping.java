package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;


import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public final class UnknownMarketplaceMapping {
	
	public final int marketplace_id;
	public final String listing_id;
	
	public UnknownMarketplaceMapping(final int marketplace_id, final String listing_id) {
		this.marketplace_id = marketplace_id;
		this.listing_id = listing_id;
	}
	
	public static Set<UnknownMarketplaceMapping> loadUnknownMarketplaceMappings() {
		final Set<UnknownMarketplaceMapping> mappings = new HashSet<>();
		
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT marketplace_id,listing_id FROM unknown_marketplace_mappings")) {
			while(res.next()) {
				mappings.add(new UnknownMarketplaceMapping(res.getInt("marketplace_id"), res.getString("listing_id")));
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return mappings;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((listing_id == null) ? 0 : listing_id.hashCode());
		result = prime * result + marketplace_id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UnknownMarketplaceMapping other = (UnknownMarketplaceMapping) obj;
		if (listing_id == null) {
			if (other.listing_id != null)
				return false;
		} else if (!listing_id.equals(other.listing_id))
			return false;
		if (marketplace_id != other.marketplace_id)
			return false;
		return true;
	}
	
}
