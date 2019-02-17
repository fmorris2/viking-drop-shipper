package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

public class MarketplaceLoader {
	
	public static void loadMarketplaces() {
		final Statement st = VDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM marketplace");
			while(results.next()) {
				
				final Marketplace marketplace = new Marketplace.Builder()
					.id(results.getInt("id"))
					.marketplace_name(results.getString("marketplace_name"))
					.marketplace_url(results.getString("marketplace_url"))
					.marketplace_profit_cut(results.getDouble("marketplace_profit_cut"))
					.build();
				
				Marketplaces.addMarketplace(marketplace);
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		}		
	}
}
