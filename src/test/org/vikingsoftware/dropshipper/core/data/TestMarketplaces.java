package test.org.vikingsoftware.dropshipper.core.data;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;

import org.junit.Assert;
import org.junit.Test;

public class TestMarketplaces {

	@Test
	public void test() {
		MarketplaceLoader.loadMarketplaces();
		for(final Marketplaces marketplace : Marketplaces.values()) {
			Assert.assertNotNull("Failed to load marketplace " + marketplace, marketplace.getMarketplace());
		}
	}

}
