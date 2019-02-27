package main.org.vikingsoftware.dropshipper.order.parser;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;

public class OrderParser implements CycleParticipant {

	@Override
	public void cycle() {
		
		MarketplaceLoader.loadMarketplaces();
		
		final Collection<CustomerOrder> newOrders = new ArrayList<>();
		
		//parse all new orders across all of our supported marketplaces
		for(final Marketplaces marketEntry : Marketplaces.values()) {
			System.out.println("Generating and executing parsing strategy for marketplace " + marketEntry);
			final OrderParsingStrategy parsingStrategy = marketEntry.generateParsingStrategy();
			final Collection<CustomerOrder> orders = parsingStrategy.parseNewOrders();
			System.out.println("Parsed " + orders.size() + " new orders for " + marketEntry);
			newOrders.addAll(orders);
		}
		
		//store all new orders in DB
		final String sql = "INSERT INTO customer_order(marketplace_listing_id, sku, sale_price, quantity, marketplace_order_id, buyer_username,"
				+ " buyer_name, buyer_country, buyer_street_address, buyer_apt_suite_unit_etc, buyer_state_province_region,"
				+ "buyer_city, buyer_zip_postal_code) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		final PreparedStatement prepared = VDSDBManager.get().createPreparedStatement(sql);
		try {
			for(final CustomerOrder order : newOrders) {
				prepared.setInt(1, order.marketplace_listing_id);
				prepared.setString(2, order.sku);
				prepared.setDouble(3, order.sale_price);
				prepared.setInt(4, order.quantity);
				prepared.setString(5, order.marketplace_order_id);
				prepared.setString(6, order.buyer_username);
				prepared.setString(7, order.buyer_name);
				prepared.setString(8, order.buyer_country);
				prepared.setString(9, order.buyer_street_address);
				prepared.setString(10, order.buyer_apt_suite_unit_etc);
				prepared.setString(11, order.buyer_state_province_region);
				prepared.setString(12, order.buyer_city);
				prepared.setString(13, order.buyer_zip_postal_code);
				prepared.addBatch();
			}
		
			final int numRows = prepared.executeBatch().length;
			System.out.println("Executed batch of " + numRows + " insert queries.");
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

}
