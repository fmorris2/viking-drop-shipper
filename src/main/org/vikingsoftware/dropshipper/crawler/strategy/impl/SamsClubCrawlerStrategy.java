package main.org.vikingsoftware.dropshipper.crawler.strategy.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import main.org.vikingsoftware.dropshipper.crawler.strategy.FulfillmentListingCrawlerStrategy;

public class SamsClubCrawlerStrategy extends FulfillmentListingCrawlerStrategy {
	
	private static final Map<String, String> CATEGORY_MAP = new HashMap<>();
	
	static {
//		CATEGORY_MAP.put("Apparel & Shoes", "1959");
//		CATEGORY_MAP.put("Appliances", "1004");
//		CATEGORY_MAP.put("Auto & Tires", "1055");
//		CATEGORY_MAP.put("Baby & Toddler", "1946");
//		CATEGORY_MAP.put("Books & Entertainment", "15740371");
//		CATEGORY_MAP.put("Cigarettes & Tobacco", "1580");
//		CATEGORY_MAP.put("Clearance", "1150109");
//		CATEGORY_MAP.put("Computers", "1116");
//		CATEGORY_MAP.put("Electronics & Computers", "1086");
//		CATEGORY_MAP.put("Furniture", "1286");
//		CATEGORY_MAP.put("Gift Cards", "1003");
//		CATEGORY_MAP.put("Grocery", "1444");
//		CATEGORY_MAP.put("Home", "1285");
//		CATEGORY_MAP.put("Home Improvement", "1390");
//		CATEGORY_MAP.put("Household Essentials", "450203");
//		CATEGORY_MAP.put("Jewelry, Flowers & Gifts", "7520117");
		CATEGORY_MAP.put("Member's Mark", "4710101");																													CATEGORY_MAP.put("New Items", "8131");
		CATEGORY_MAP.put("Office", "1706");
		CATEGORY_MAP.put("Outdoor & Patio", "1852");
		CATEGORY_MAP.put("Pet Supplies", "2011");
		CATEGORY_MAP.put("Pharmacy, Health & Beauty", "1585");
		CATEGORY_MAP.put("Photo Center", "1020193");
		CATEGORY_MAP.put("Restaurant Supplies", "2209");
		CATEGORY_MAP.put("Seasonal & Occasions", "1900101");
		CATEGORY_MAP.put("Shop by Business", "100002");
		CATEGORY_MAP.put("Shops & Promotions", "7130108");
		CATEGORY_MAP.put("Tech Savings", "1240109");
		CATEGORY_MAP.put("Toys & Games", "1929");
	}
	
	public void crawl() {
		final SamsCategoryAPI api = new SamsCategoryAPI();
		System.out.println("[SamsClubCrawlerStrategy] - crawl");
		for(final Map.Entry<String, String> category : CATEGORY_MAP.entrySet()) {
			System.out.println("Parsing products from category: " + category.getKey());
			if(api.parse(category.getValue())) {
				final Set<String> urls = api.getProductUrls();
				System.out.println("\tParsed " + urls.size() + " urls");
				for(final String url : urls) {
					urlFound(url);
				}		
			}
			
		}
	}

}
