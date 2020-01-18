package test.org.vikingsoftware.dropshipper.core.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class TestNordVPNProxy {

	@Test
	public void test() {
		try {
			final WrappedHttpClient client = HttpClientManager.get().getClient();
			final HttpGet get = new HttpGet("https://www.samsclub.com/api/soa/services/v1/catalog/product/prod20651260?response_group=LARGE&clubId=6279");
			System.out.println("Current proxy: " + client.proxy);
			final HttpResponse response = client.execute(get);
			System.out.println("Response: " + EntityUtils.toString(response.getEntity()));
			Assert.assertTrue(response.getStatusLine().getStatusCode() == 200);
		} catch(final Exception e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}
