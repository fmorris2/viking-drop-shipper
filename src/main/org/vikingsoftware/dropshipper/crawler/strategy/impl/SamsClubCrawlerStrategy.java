package main.org.vikingsoftware.dropshipper.crawler.strategy.impl;

import java.util.LinkedList;
import java.util.Queue;

import main.org.vikingsoftware.dropshipper.crawler.strategy.FulfillmentListingCrawlerStrategy;

public class SamsClubCrawlerStrategy implements FulfillmentListingCrawlerStrategy {

	private static final String ALL_CATEGORIES_URL = "https://www.samsclub.com/c?xid=hdr_shop1_all-departments";
	
	private Queue<String> categoryURLs = new LinkedList<>();
	
		
	@Override
	public String findURL() {
		return null;
	}

}
