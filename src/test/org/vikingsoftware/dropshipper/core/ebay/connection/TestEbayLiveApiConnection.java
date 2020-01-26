package test.org.vikingsoftware.dropshipper.core.ebay.connection;

import java.util.Calendar;

import main.org.vikingsoftware.dropshipper.core.ebay.EbayApiContextManager;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.call.GeteBayOfficialTimeCall;

public class TestEbayLiveApiConnection {

	@Test
	public void test() {
		try {
			final ApiContext apiContext = EbayApiContextManager.getLiveContext();
			final GeteBayOfficialTimeCall apiCall = new GeteBayOfficialTimeCall(apiContext);
			final Calendar cal = apiCall.geteBayOfficialTime();
			Assert.assertNotNull(cal.getTime().toString());
		} catch(final Exception e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}

}
