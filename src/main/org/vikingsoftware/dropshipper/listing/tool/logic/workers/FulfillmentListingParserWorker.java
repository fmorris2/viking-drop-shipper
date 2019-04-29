package main.org.vikingsoftware.dropshipper.listing.tool.logic.workers;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParsingManager;

public class FulfillmentListingParserWorker extends SwingWorker<Void, String> {

	private static final long CYCLE_TIME = 50;

	private static FulfillmentListingParserWorker instance;

	private final Queue<String> urlQueue = new LinkedList<>();

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
		updateQueueSizeLabel();
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
				FulfillmentParsingManager.parseListing(urlQueue.peek());
				urlQueue.poll();
				updateQueueSizeLabel();
			}
			Thread.sleep(CYCLE_TIME);
		}
	}

	private void updateQueueSizeLabel() {
		SwingUtilities.invokeLater(() -> ListingToolGUI.get().queueSizeValue.setText(Integer.toString(urlQueue.size())));
	}

}
