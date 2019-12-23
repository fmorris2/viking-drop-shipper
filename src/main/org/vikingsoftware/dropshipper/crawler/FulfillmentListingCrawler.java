package main.org.vikingsoftware.dropshipper.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
	
	public static void main(final String[] args) throws IOException {
		final Set<String> savedUrls = new HashSet<>();
		try(final FileReader fR = new FileReader("sams-club-urls.txt");
			final BufferedReader bR = new BufferedReader(fR)) {
			String line;
			while((line = bR.readLine()) != null) {
				savedUrls.add(line);
			}
		}
		
		System.out.println("Loaded " + savedUrls.size() + " saved URLs");
		final FileWriter fW = new FileWriter("sams-club-urls.txt", true);
		final BufferedWriter bW = new BufferedWriter(fW);
		new FulfillmentListingCrawler().start(url -> {
			try {
				if(!savedUrls.contains(url)) {
					bW.write(url);
					bW.newLine();
					bW.flush();
					savedUrls.add(url);
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}
		});
		
	}
	
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
			try {
				System.out.println("About to crawl using strategy " + strategy);
				strategy.crawl();
				System.out.println("Done crawling using strategy " + strategy);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("No longer crawling with strategy " + strategy);
	}
	
}
