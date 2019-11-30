package main.org.vikingsoftware.dropshipper.order.parser;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.TransactionUtils;
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
		final String sql = "INSERT INTO customer_order(marketplace_listing_id, sku, sell_listing_price,"
				+ " sell_shipping, sell_percentage_cut, sell_total,"
				+ " quantity, marketplace_order_id, buyer_username,"
				+ " buyer_name, buyer_country, buyer_street_address, buyer_apt_suite_unit_etc, buyer_state_province_region,"
				+ "buyer_city, buyer_zip_postal_code, buyer_phone_number, date_parsed, handling_time) "
				+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		final PreparedStatement prepared = VSDSDBManager.get().createPreparedStatement(sql);
		try {
			for(final CustomerOrder order : newOrders) {
				prepared.setInt(1, order.marketplace_listing_id);
				prepared.setString(2, order.sku);
				prepared.setDouble(3, order.sell_listing_price);
				prepared.setDouble(4, order.sell_shipping);
				prepared.setDouble(5, order.sell_percentage_cut);
				prepared.setDouble(6, order.sell_total);
				prepared.setInt(7, order.quantity);
				prepared.setString(8, order.marketplace_order_id);
				prepared.setString(9, order.buyer_username);
				prepared.setString(10, order.buyer_name);
				prepared.setString(11, order.buyer_country);
				prepared.setString(12, order.buyer_street_address);
				prepared.setString(13, order.buyer_apt_suite_unit_etc);
				prepared.setString(14, order.buyer_state_province_region);
				prepared.setString(15, order.buyer_city);
				prepared.setString(16, order.buyer_zip_postal_code);
				prepared.setString(17, order.buyer_phone_number);
				prepared.setLong(18, System.currentTimeMillis());
				prepared.setInt(19, order.handling_time);
				prepared.addBatch();
			}

			final int numRows = prepared.executeBatch().length;
			System.out.println("Executed batch of " + numRows + " insert queries.");
			
			for(final CustomerOrder newOrder : newOrders) {
				final Optional<CustomerOrder> orderWithId = CustomerOrderManager.loadCustomerOrderByMarketplaceOrderId(newOrder.marketplace_order_id);
				orderWithId.ifPresent(order -> {
					System.out.println("decrementing current ebay inventory for marketplace listing id: " + order.marketplace_listing_id);
					MarketplaceListing.decrementCurrentEbayInventory(order.marketplace_listing_id);
					
					System.out.println("inserting marketplace income & marketplace sell-fee transactions for customer order w/ order id " + order.marketplace_order_id);
					TransactionUtils.insertTransactionsForCustomerOrder((float)(double)newOrder.marketplace_sell_fee, order);
				});
				
			}
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

}
