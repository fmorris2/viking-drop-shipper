package main.org.vikingsoftware.dropshipper.core.data.customer.order;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class CustomerOrderManager {
	
	private static final Set<Pair<Integer, String>> refundedOrderUpdates = new HashSet<>(); //We've marked these orders as refunded since runtime

	public static List<CustomerOrder> loadOrdersToExecute() {
		final List<CustomerOrder> toExecute = new ArrayList<>();
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order"
					+ " LEFT JOIN processed_order ON customer_order.id=processed_order.customer_order_id"
					+ " WHERE processed_order.customer_order_id IS NULL AND customer_order.is_cancelled=0")) {

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
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order"
					+ " WHERE id="+id)) {

			if(results.next()) {
				return Optional.of(buildOrderFromResultSet(results));
			}
		} catch(final Exception e) {
			DBLogging.medium(CustomerOrder.class, "failed to load customer order by id " + id, e);
		}

		return Optional.empty();
	}
	
	public static Optional<CustomerOrder> loadCustomerOrderByMarketplaceOrderId(final String orderId) {
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order"
					+ " WHERE marketplace_order_id='"+orderId+"'")) {

			if(results.next()) {
				return Optional.of(buildOrderFromResultSet(results));
			}
		} catch(final Exception e) {
			DBLogging.medium(CustomerOrder.class, "failed to load customer order by marketplace order id " + orderId, e);
		}

		return Optional.empty();
	}

	public static CustomerOrder loadFirstCustomerOrder() {
		CustomerOrder toReturn = null;
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet results = st.executeQuery("SELECT * FROM customer_order LIMIT 1")) {
			if(results.next()) {
				toReturn = buildOrderFromResultSet(results);
			}
		} catch(final SQLException e) {
			e.printStackTrace();
		}

		return toReturn;
	}
	
	public static boolean setOrderRefunded(final int marketplaceListingId, final String transactionId) {
		if(refundedOrderUpdates.contains(new Pair<>(marketplaceListingId, transactionId))) {
			return true;
		}
		try(final Statement st = VSDSDBManager.get().createStatement()) {
			System.out.println("Marking customer order w/ transaction id as refunded:" + transactionId);
			st.execute("UPDATE customer_order SET is_refunded=1 WHERE marketplace_order_id="+transactionId
					+ " AND marketplace_listing_id="+marketplaceListingId + " AND is_resolved=0");
			refundedOrderUpdates.add(new Pair<>(marketplaceListingId, transactionId));
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private static CustomerOrder buildOrderFromResultSet(final ResultSet results) throws SQLException {
		final int snapshot_fulfillment_quantity_multiplier = results.getInt("snapshot_fulfillment_quantity_multiplier");
		return new CustomerOrder.Builder()
			.id(results.getInt("id"))
			.marketplace_listing_id(results.getInt("marketplace_listing_id"))
			.sku(results.getString("sku"))
			.sell_listing_price(results.getDouble("sell_listing_price"))
			.sell_shipping(results.getDouble("sell_shipping"))
			.sell_percentage_cut(results.getDouble("sell_percentage_cut"))
			.sell_total(results.getDouble("sell_total"))
			.quantity(results.getInt("quantity"))
			.fulfillment_purchase_quantity(results.getInt("quantity") * snapshot_fulfillment_quantity_multiplier)
			.marketplace_order_id(results.getString("marketplace_order_id"))
			.buyer_username(results.getString("buyer_username"))
			.buyer_name(results.getString("buyer_name"))
			.buyer_country(results.getString("buyer_country"))
			.buyer_street_address(results.getString("buyer_street_address"))
			.buyer_apt_suite_unit_etc(results.getString("buyer_apt_suite_unit_etc"))
			.buyer_state_province_region(results.getString("buyer_state_province_region"))
			.buyer_city(results.getString("buyer_city"))
			.buyer_zip_postal_code(results.getString("buyer_zip_postal_code"))
			.buyer_phone_number(results.getString("buyer_phone_number"))
			.date_parsed(results.getLong("date_parsed"))
			.date_cancelled(results.getLong("date_cancelled"))
			.is_cancelled(results.getBoolean("is_cancelled"))
			.handling_time(results.getInt("handling_time"))
			.snapshot_fulfillment_quantity_multiplier(snapshot_fulfillment_quantity_multiplier)
			.is_refunded(results.getBoolean("is_refunded"))
			.is_resolved(results.getBoolean("is_resolved"))
			.build();
	}
}
