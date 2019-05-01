package main.org.vikingsoftware.dropshipper.listing.tool.logic.workers;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParsingManager;

public class FulfillmentListingParserWorker extends SwingWorker<Void, String> {

	private static final long CYCLE_TIME = 50;
	private static final int LISTING_ATTEMPT_THRESHOLD = 3;

	private static FulfillmentListingParserWorker instance;

	private final Queue<String> urlQueue = new LinkedList<>();

	private int attempts;

	private FulfillmentListingParserWorker() {
		execute();
	}

	public static synchronized FulfillmentListingParserWorker instance() {
		if(instance == null) {
			instance = new FulfillmentListingParserWorker();
		}

		return instance;
	}

	public void addUrlToQueue(final String url) {
		urlQueue.add(url);
	}

	public void updateStatus(final String status) {
		this.publish(status);
	}

	@Override
	protected void process(List<String> chunks) {
		if(!chunks.isEmpty()) {
			final String status = chunks.get(chunks.size() - 1);
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusText.setText(status));
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		while(true) {
			if(!urlQueue.isEmpty()) {
				final Listing listing = FulfillmentParsingManager.parseListing(urlQueue.peek());
				if(listing != null) {
					final boolean shouldDisplayListing = ListingQueue.isEmpty();
					ListingQueue.add(listing);
					if(shouldDisplayListing) {
						ListingToolGUI.getController().displayNextListing();
					}
					urlQueue.poll();
					attempts = 0;
				} else if(attempts > LISTING_ATTEMPT_THRESHOLD) {
					System.out.println("Failed to parse URL: " + urlQueue.poll());
					attempts = 0;
				} else {
					attempts++;
				}
			}
			Thread.sleep(CYCLE_TIME);
		}
	}
}
