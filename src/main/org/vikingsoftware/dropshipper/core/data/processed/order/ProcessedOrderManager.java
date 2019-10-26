package main.org.vikingsoftware.dropshipper.core.data.processed.order;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public final class ProcessedOrderManager {
	
	public static Optional<ProcessedOrder> getProcessedOrderForCustomerOrder(final int customerOrderId) {
		try (final Statement st = VSDSDBManager.get().createStatement();
			 final ResultSet res = st.executeQuery("SELECT * FROM processed_order WHERE customer_order_id="+customerOrderId)) {
			if(res.next()) {
				final ProcessedOrder order = new ProcessedOrder.Builder()
						.id(res.getInt("id"))
						.customer_order_id(res.getInt("customer_order_id"))
						.fulfillment_listing_id(res.getInt("fulfillment_listing_id"))
						.fulfillment_account_id(res.getInt("fulfillment_account_id"))
						.fulfillment_transaction_id(res.getString("fulfillment_transaction_id"))
						.tracking_number(res.getString("tracking_number"))
						.buy_subtotal(res.getDouble("buy_subtotal"))
						.buy_sales_tax(res.getDouble("buy_sales_tax"))
						.buy_shipping(res.getDouble("buy_shipping"))
						.buy_product_fees(res.getDouble("buy_product_fees"))
						.buy_total(res.getDouble("buy_total"))
						.profit(res.getDouble("profit"))
						.date_processed(res.getLong("date_processed"))
						.date_cancelled(res.getLong("date_cancelled"))
						.is_cancelled(res.getBoolean("is_cancelled"))
						.build();
				
				return Optional.of(order);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return Optional.empty();
	}
}
