package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.util.HashMap;
import java.util.Map;

public enum Marketplaces {
	EBAY;
	
	private static Map<Integer, Marketplace> MARKETPLACES = new HashMap<>();
	
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
