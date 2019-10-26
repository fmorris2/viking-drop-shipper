package main.org.vikingsoftware.dropshipper.core.data.transaction;

import java.sql.Statement;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public final class TransactionManager {
	
	public static boolean insertTransaction(final Transaction transaction) {
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			return st.execute(generateInsertQuery(transaction));
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static final String generateInsertQuery(final Transaction transaction) {
		return "INSERT INTO transaction(type,amount,customer_order_id,processed_order_id,date) VALUES("
				+ transaction.type.ordinal() + "," + transaction.amount + "," + convertIntegerToSqlQueryForm(transaction.customerOrderId)
				+ "," + convertIntegerToSqlQueryForm(transaction.processedOrderId) + "," + transaction.date;
	}
	
	private static final String convertIntegerToSqlQueryForm(final Integer val) {
		return val == null ? "NULL" : val.toString();
	}
}
