package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.util.HashMap;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.impl.EbayOrderParsingStrategy;

public enum Marketplaces {
	
	EBAY(EbayOrderParsingStrategy.class);
	
	private static Map<Integer, Marketplace> MARKETPLACES = new HashMap<>();
	
	private final Class<? extends OrderParsingStrategy> parsingStrategy;
	
	Marketplaces(final Class<? extends OrderParsingStrategy> parsingStrategy) {
		this.parsingStrategy = parsingStrategy;
	}
	
	public static void addMarketplace(final Marketplace marketplace) {
		MARKETPLACES.put(marketplace.id, marketplace);
	}
	
	public OrderParsingStrategy generateParsingStrategy() {
		try {
			return parsingStrategy.newInstance();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public Marketplace getMarketplace() {
		return MARKETPLACES.get(getMarketplaceId());
	}
	
	public int getMarketplaceId() {
		return ordinal() + 1;
	}
}
