package test.org.vikingsoftware.dropshipper.core.net;

import org.jsoup.nodes.Document;
import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.core.net.ConnectionManager;

public class TestNordVPNProxy {

	@Test
	public void test() {
		try {
			final Document doc = ConnectionManager.get().getConnection()
			  .url("http://www.samsclub.com/api/soa/services/v1/catalog/product/prod20651260?response_group=LARGE&clubId=6279")
		      .ignoreContentType(true)
		      .get();
			System.out.println(doc.text());
		
			Assert.assertTrue(!doc.data().isEmpty());
		} catch(final Exception e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}
