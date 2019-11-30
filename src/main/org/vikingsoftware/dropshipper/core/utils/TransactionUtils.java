package main.org.vikingsoftware.dropshipper.core.utils;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.transaction.Transaction;
import main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public final class TransactionUtils {
	
	private static final String INSERT_QUERY = "INSERT INTO transaction(type,amount,customer_order_id,"
			+ "processed_order_id,date) VALUES (?,?,?,?,?)";
	
	private TransactionUtils() {
		//utils classes need not be instantiated
	}
	
	public static boolean insertTransactionsForCustomerOrder(final float marketplaceSellFee, final CustomerOrder order) {		
		try {		
			final Transaction marketplaceIncomeTransaction = new Transaction.Builder()
					.type(TransactionType.MARKETPLACE_INCOME)
					.amount((float)order.sell_total)
					.customerOrderId(order.id)
					.date(order.date_parsed)
					.build();
			
			final Transaction marketplaceSellFeeTransaction = new Transaction.Builder()
					.type(TransactionType.MARKETPLACE_SELL_FEE)
					.amount(marketplaceSellFee)
					.customerOrderId(order.id)
					.date(order.date_parsed)
					.build();
			
			return TransactionUtils.insertTransaction(marketplaceIncomeTransaction)
			   && TransactionUtils.insertTransaction(marketplaceSellFeeTransaction);
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.critical(TransactionUtils.class, "Failed to insert transactions for new customer order " + order.id, e);
		}
				
		return false;
	}
	
	public static boolean insertTransactionForProcessedOrder(final ProcessedOrder order) {
		boolean success = false;
		try {
			final Optional<Integer> id = order.loadIdFromDB();
			if(id.isPresent()) {
				final Transaction fulfillmentCostTransaction = new Transaction.Builder()
						.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.FULFILLMENT_COST)
						.amount((float)order.buy_total * -1)
						.customerOrderId(order.customer_order_id)
						.processedOrderId(id.get())
						.date(order.date_processed)
						.build();
				success = TransactionUtils.insertTransaction(fulfillmentCostTransaction);
			}
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.critical(TransactionUtils.class, "Failed to insert transaction for successful processed order " + order.id, e);
		}
		
		return success;
	}
	
	public static boolean insertTransaction(final Transaction transaction) {
		final PreparedStatement st = VSDSDBManager.get().createPreparedStatement(INSERT_QUERY);
		try {
			st.setInt(1, transaction.type.ordinal());
			st.setFloat(2, transaction.amount);
			
			if(transaction.customerOrderId == null) {
				st.setNull(3, Types.INTEGER);
			} else {
				st.setInt(3, transaction.customerOrderId);
			}
			
			if(transaction.processedOrderId == null) {
				st.setNull(4, Types.INTEGER);
			} else {
				st.setInt(4, transaction.processedOrderId);
			}
			
			st.setLong(5, transaction.date);
			
			st.execute();
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	} 
}
