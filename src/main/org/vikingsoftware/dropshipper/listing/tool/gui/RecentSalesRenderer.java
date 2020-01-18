package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.org.vikingsoftware.dropshipper.core.web.DefaultWebDriver;

public class RecentSalesRenderer extends DefaultWebDriver {
	
	private static final long CYCLE_TIME = 1000;
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Stack<String> queuedUrls = new Stack<>();
	
	public RecentSalesRenderer() {
		executor.execute(this::updateBrowser);
	}
	
	private void updateBrowser() {
		while(true) {
			try {
				synchronized(queuedUrls) {
					if(!queuedUrls.isEmpty()) {
						super.get(queuedUrls.pop());
						queuedUrls.clear();
					}
				}
				Thread.sleep(CYCLE_TIME);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void get(String url) {
		synchronized(queuedUrls) {
			queuedUrls.push(url);
		}
	}
}
