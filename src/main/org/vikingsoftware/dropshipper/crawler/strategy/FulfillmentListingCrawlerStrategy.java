package main.org.vikingsoftware.dropshipper.crawler.strategy;

import java.util.ArrayList;
import java.util.List;

import main.org.vikingsoftware.dropshipper.crawler.FulfillmentListingCrawlerListener;

public abstract class FulfillmentListingCrawlerStrategy {
	
	private List<FulfillmentListingCrawlerListener> listeners = new ArrayList<>();
	
	public abstract void crawl();
	
	public void addCrawlListener(final FulfillmentListingCrawlerListener listener) {
		listeners.add(listener);
	}
	
	protected void urlFound(final String url) {
		for(final FulfillmentListingCrawlerListener listener : listeners) {
			listener.urlFound(url);
		}
	}
}
