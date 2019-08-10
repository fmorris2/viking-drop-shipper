package main.org.vikingsoftware.dropshipper.crawler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.org.vikingsoftware.dropshipper.crawler.strategy.FulfillmentListingCrawlerStrategy;
import main.org.vikingsoftware.dropshipper.crawler.strategy.impl.SamsClubCrawlerStrategy;

public class FulfillmentListingCrawler {

	private final FulfillmentListingCrawlerStrategy[] strategies = {
		new SamsClubCrawlerStrategy()
	};
	
	private final ExecutorService executor = Executors.newFixedThreadPool(strategies.length);
	
	private boolean isCrawling = true;
	
	/*
	 * Start the crawler. Every time a URL is retrieved by one of the strategies,
	 * call the provided listener
	 */
	public void start(final FulfillmentListingCrawlerListener listener) {
		for(final FulfillmentListingCrawlerStrategy strategy : strategies) {
			strategy.addCrawlListener(listener);
			executor.execute(() -> runCrawlStrategy(strategy));
		}
	}
	
	public void stop() {
		isCrawling = false;
	}
	
	private void runCrawlStrategy(final FulfillmentListingCrawlerStrategy strategy) {
		while(isCrawling) {
			strategy.crawl();
		}
	}
	
}
