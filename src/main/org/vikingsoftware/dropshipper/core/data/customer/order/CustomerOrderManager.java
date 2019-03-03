package main.org.vikingsoftware.dropshipper.core.data.customer.order;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class CustomerOrderManager {
	
	public static List<CustomerOrder> loadOrdersToExecute() {
		final List<CustomerOrder> toExecute = new ArrayList<>();
		try {
			final Statement st = VDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order"
					+ " LEFT JOIN processed_orders ON customer_order.id=processed_orders.customer_order_id"
					+ " WHERE processed_orders.customer_order_id IS NULL");
			
			while(results.next()) {
				toExecute.add(buildOrderFromResultSet(results));
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("loaded " + toExecute.size() + " orders to execute");
		return toExecute;
	}
	
	public static Optional<CustomerOrder> loadCustomerOrderById(final int id) {
		try {
			final Statement st = VDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order"
					+ " WHERE id="+id);
			
			if(results.next()) {
				return Optional.of(buildOrderFromResultSet(results));
			}
		} catch(final Exception e) {
			DBLogging.medium(CustomerOrder.class, "failed to load customer order by id " + id, e);
		}
		
		return Optional.empty();
	}

	public static CustomerOrder loadFirstCustomerOrder() {
		CustomerOrder toReturn = null;
		final Statement st = VDSDBManager.get().createStatement();
		try {
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order LIMIT 1");
			if(results.next()) {
				toReturn = buildOrderFromResultSet(results);		
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}
		
		return toReturn;
	}
	
	private static CustomerOrder buildOrderFromResultSet(final ResultSet results) throws SQLException {
		return new CustomerOrder.Builder()
			.id(results.getInt("id"))
			.marketplace_listing_id(results.getInt("marketplace_listing_id"))
			.sku(results.getString("sku"))
			.sale_price(results.getDouble("sale_price"))
			.quantity(results.getInt("quantity"))
			.marketplace_order_id(results.getString("marketplace_order_id"))
			.buyer_username(results.getString("buyer_username"))
			.buyer_name(results.getString("buyer_name"))
			.buyer_country(results.getString("buyer_country"))
			.buyer_street_address(results.getString("buyer_street_address"))
			.buyer_apt_suite_unit_etc(results.getString("buyer_apt_suite_unit_etc"))
			.buyer_state_province_region(results.getString("buyer_state_province_region"))
			.buyer_city(results.getString("buyer_city"))
			.buyer_zip_postal_code(results.getString("buyer_zip_postal_code"))
			.build();
	}
}
