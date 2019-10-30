package test.org.vikingsoftware.dropshipper.core.ebay.seller;

import org.junit.Test;

import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.TransactionType;

import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;

public class TestHandleCombinedInvoice {

	@Test
	public void test() {
			final OrderType order = EbayCalls.getOrder("17-03823-56737");
			if(order != null) {
				System.out.println("Amount Paid: " + order.getAmountPaid().getValue());
				final TransactionType[] transactions = order.getTransactionArray().getTransaction();
				if(transactions != null) {
					System.out.println("# Transactions: " + transactions.length);
					for(final TransactionType transaction : transactions) {
						System.out.println("\t"+transaction.getOrderLineItemID());
						System.out.println("\t"+transaction.getTransactionPrice().getValue());
					}
				}
			} else {
				System.out.println("Order is null!");
			}
	}
}
