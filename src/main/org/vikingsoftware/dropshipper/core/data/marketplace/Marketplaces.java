package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.util.HashMap;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.inventory.AutomaticInventoryUpdater;
import main.org.vikingsoftware.dropshipper.inventory.impl.EbayInventoryUpdater;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.impl.EbayOrderParsingStrategy;

public enum Marketplaces {
	
	EBAY(EbayOrderParsingStrategy.class, EbayInventoryUpdater.class);
	
	private static Map<Integer, Marketplace> MARKETPLACES = new HashMap<>();
	
	private final Class<? extends OrderParsingStrategy> parsingStrategy;
	private final Class<? extends AutomaticInventoryUpdater> inventoryUpdater;
	
	Marketplaces(final Class<? extends OrderParsingStrategy> parsingStrategy,
			final Class<? extends AutomaticInventoryUpdater> inventoryUpdater) {
		this.parsingStrategy = parsingStrategy;
		this.inventoryUpdater = inventoryUpdater;
	}
	
	public static void addMarketplace(final Marketplace marketplace) {
		MARKETPLACES.put(marketplace.id, marketplace);
	}
	
	public static Marketplaces getById(final int id) {
		return values()[id - 1];
	}
	
	public OrderParsingStrategy generateParsingStrategy() {
		try {
			return parsingStrategy.newInstance();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public AutomaticInventoryUpdater generateInventoryUpdater() {
		try {
			return inventoryUpdater.newInstance();
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
