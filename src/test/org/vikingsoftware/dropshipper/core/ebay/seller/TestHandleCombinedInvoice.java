package test.org.vikingsoftware.dropshipper.core.ebay.seller;

import java.util.Arrays;

import org.junit.Test;

import com.ebay.soap.eBLBaseComponents.OrderType;
import com.ebay.soap.eBLBaseComponents.TransactionType;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
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
	
	@Test
	public void testWithOrderParserLogic() {
		MarketplaceLoader.loadMarketplaces();
		CustomerOrder[] orders = EbayCalls.getOrdersLastXDays(90);
		orders = Arrays.stream(orders).filter(order -> "1130567422024".equals(order.marketplace_order_id)).toArray(CustomerOrder[]::new);
		
		System.out.println(orders[0].sell_total);
	}
}
