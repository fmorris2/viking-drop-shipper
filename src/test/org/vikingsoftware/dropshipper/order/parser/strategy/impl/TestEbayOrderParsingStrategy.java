package test.org.vikingsoftware.dropshipper.order.parser.strategy.impl;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.impl.EbayOrderParsingStrategy;

import org.junit.Assert;
import org.junit.Test;

public class TestEbayOrderParsingStrategy {

	@Test
	public void test() {
		MarketplaceLoader.loadMarketplaces();
		Marketplaces.EBAY.getMarketplace().clearKnownOrderIds();
		Assert.assertFalse("Failed to parse Ebay orders", new EbayOrderParsingStrategy().parseNewOrders().isEmpty());
	}

}
