package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.util.HashMap;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.impl.EbayOrderParsingStrategy;

public enum Marketplaces {
	
	EBAY(new EbayOrderParsingStrategy());
	
	private static Map<Integer, Marketplace> MARKETPLACES = new HashMap<>();
	
	public final OrderParsingStrategy parsingStrategy;
	
	Marketplaces(final OrderParsingStrategy parsingStrategy) {
		this.parsingStrategy = parsingStrategy;
	}
	
	public static void addMarketplace(final Marketplace marketplace) {
		MARKETPLACES.put(marketplace.id, marketplace);
	}
	
	public Marketplace getMarketplace() {
		return MARKETPLACES.get(getMarketplaceId());
	}
	
	public int getMarketplaceId() {
		return ordinal() + 1;
	}
}
