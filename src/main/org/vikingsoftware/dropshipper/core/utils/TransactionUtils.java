package main.org.vikingsoftware.dropshipper.core.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.transaction.Transaction;
import main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public final class TransactionUtils {
	
	private static final String INSERT_QUERY = "INSERT INTO transaction(type,amount,customer_order_id,"
			+ "processed_order_id,marketplace_listing_id,date,notes) VALUES (?,?,?,?,?,?,?)";
	
	private TransactionUtils() {
		//utils classes need not be instantiated
	}
	
	public static boolean insertTransactionsForCustomerOrder(final CustomerOrder order) {		
		try {		
			final Transaction marketplaceIncomeTransaction = new Transaction.Builder()
					.type(TransactionType.MARKETPLACE_INCOME)
					.amount((float)order.sell_total)
					.customer_order_id(order.id)
					.marketplace_listing_id(order.marketplace_listing_id)
					.date(order.date_parsed)
					.build();
			
			boolean successfullyInsertedPaymentProcessorFee = true;
			if(order.payment_processor_fee != null && order.payment_processor_fee > 0 && order.payment_processor_fee_date != null) {
				final Transaction trans = new Transaction.Builder()
					.type(TransactionType.PAYMENT_PROCESSOR_FEE)
					.amount(order.payment_processor_fee.floatValue())
					.customer_order_id(order.id)
					.marketplace_listing_id(order.marketplace_listing_id)
					.date(order.payment_processor_fee_date)
					.build();
				
				successfullyInsertedPaymentProcessorFee = TransactionUtils.insertTransaction(trans);
			}
			
			boolean successfullyInsertedMarketplaceFee = true;
			if(order.marketplace_sell_fee != null && order.marketplace_sell_fee > 0) {
				final Transaction trans = new Transaction.Builder()
						.type(TransactionType.MARKETPLACE_SELL_FEE)
						.amount(order.marketplace_sell_fee.floatValue())
						.customer_order_id(order.id)
						.marketplace_listing_id(order.marketplace_listing_id)
						.date(order.date_parsed)
						.build();
					
				successfullyInsertedMarketplaceFee = TransactionUtils.insertTransaction(trans);
			}
			
			return TransactionUtils.insertTransaction(marketplaceIncomeTransaction)
			   && successfullyInsertedPaymentProcessorFee && successfullyInsertedMarketplaceFee;
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.critical(TransactionUtils.class, "Failed to insert transactions for new customer order " + order.id, e);
		}
				
		return false;
	}
	
	public static boolean insertTransactionForProcessedOrder(final CustomerOrder customerOrder, final ProcessedOrder processedOrder) {
		boolean success = false;
		try {
			final Optional<Integer> id = processedOrder.loadIdFromDB();
			if(id.isPresent()) {
				final Transaction fulfillmentCostTransaction = new Transaction.Builder()
						.type(main.org.vikingsoftware.dropshipper.core.data.transaction.TransactionType.FULFILLMENT_COST)
						.amount((float)processedOrder.buy_total * -1)
						.marketplace_listing_id(customerOrder.marketplace_listing_id)
						.customer_order_id(processedOrder.customer_order_id)
						.processed_order_id(id.get())
						.date(processedOrder.date_processed)
						.build();
				success = TransactionUtils.insertTransaction(fulfillmentCostTransaction);
				success = TransactionUtils.updateProcessedOrderIdForExistingTransactions(processedOrder.customer_order_id, id.get()) && success;
			}
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.critical(TransactionUtils.class, "Failed to insert transaction for successful processed order " + processedOrder.id, e);
		}
		
		return success;
	}
	
	public static boolean insertTransaction(final Transaction transaction) {		
		boolean alreadyExists = true;
		
		try (final Statement selectSt = VSDSDBManager.get().createStatement();
			 final ResultSet rs = selectSt.executeQuery(generateSelectQuery(transaction))){
				alreadyExists = rs.next();
		} catch(final Exception e) {
 			e.printStackTrace();
		}
		
		if(!alreadyExists) {
			final PreparedStatement st = VSDSDBManager.get().createPreparedStatement(INSERT_QUERY);
			try {
				st.setInt(1, transaction.type.value);
				st.setFloat(2, transaction.amount);
				
				if(transaction.customer_order_id == null) {
					st.setNull(3, Types.INTEGER);
				} else {
					st.setInt(3, transaction.customer_order_id);
				}
				
				if(transaction.processed_order_id == null) {
					st.setNull(4, Types.INTEGER);
				} else {
					st.setInt(4, transaction.processed_order_id);
				}
				
				if(transaction.marketplace_listing_id == null) {
					st.setNull(5, Types.INTEGER);
				} else {
					st.setInt(5, transaction.marketplace_listing_id);
				}
				
				st.setLong(6, transaction.date);
				st.setString(7, transaction.notes);
				
				System.out.println("Inserting transaction of type " + transaction.type + " of amount " + transaction.amount);
				st.execute();
				return true;
			} catch(final Exception e) {
				e.printStackTrace();
			} finally {
				try {
					st.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("Transaction already exists... skipping insertion");
		}
		
		return false;
	}
	
	private static String generateSelectQuery(final Transaction transaction) {
		String query = "SELECT id FROM transaction WHERE type="+transaction.type.value
				+ " AND amount="+transaction.amount+" AND date="+transaction.date
				+ " AND " + generateNullableEqualityCheck("customer_order_id", transaction.customer_order_id)
				+ " AND " + generateNullableEqualityCheck("processed_order_id", transaction.processed_order_id)
				+ " AND " + generateNullableEqualityCheck("marketplace_listing_id", transaction.marketplace_listing_id);
		
		return query;
	}
	
	private static String generateNullableEqualityCheck(final String col, final Integer val) {
		return val == null ? col + " IS NULL" : col + "=" + val;
	}
	
	private static boolean updateProcessedOrderIdForExistingTransactions(final int customerOrderId, final int processedOrderId) {
		try(final Statement st = VSDSDBManager.get().createStatement()) {
			st.execute("UPDATE transaction SET processed_order_id="+processedOrderId + " WHERE customer_order_id="+customerOrderId);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.critical(TransactionUtils.class, "Failed to update processed order id for existing transactions " + processedOrderId, e);
		}
		
		return false;
	}
}
