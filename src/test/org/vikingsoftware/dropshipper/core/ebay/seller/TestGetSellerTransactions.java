package test.org.vikingsoftware.dropshipper.core.ebay.seller;

import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.TimeFilter;
import com.ebay.sdk.call.GetSellerTransactionsCall;
import com.ebay.soap.eBLBaseComponents.TransactionType;

public class TestGetSellerTransactions {
	
	@Test
	public void test() {
		try {
			final ApiContext apiContext = EbayApiContextManager.getLiveContext();
			final GetSellerTransactionsCall call = new GetSellerTransactionsCall(apiContext);
			call.setTimeFilter(new TimeFilter(null, null)); //will use number of days filter, defaults to 30
			final TransactionType[] transactions = call.getSellerTransactions();
			Assert.assertTrue(transactions != null && transactions.length > 0);	
		} catch(final Exception e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}

}
