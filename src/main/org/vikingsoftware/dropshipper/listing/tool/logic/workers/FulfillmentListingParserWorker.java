package main.org.vikingsoftware.dropshipper.listing.tool.logic.workers;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParsingManager;

public class FulfillmentListingParserWorker extends SwingWorker<Void, String> {

	private static final long CYCLE_TIME = 50;
	private static final int LISTING_ATTEMPT_THRESHOLD = 2;
	private static final Set<String> preExistingFulfillmentURLs = new HashSet<>();
	private static final Map<Integer, Set<String>> platformToFulfillmentIds = new HashMap<>();

	private static FulfillmentListingParserWorker instance;

	private final Queue<String> urlQueue = new LinkedList<>();

	private int attempts;

	private FulfillmentListingParserWorker() {
		execute();
		loadPreExistingFulfillmentURLs();
	}

	public static synchronized FulfillmentListingParserWorker instance() {
		if(instance == null) {
			instance = new FulfillmentListingParserWorker();
		}

		return instance;
	}



	public static boolean isPreExistingItemId(final int platform, final String id) {
		return platformToFulfillmentIds.getOrDefault(platform, new HashSet<>()).contains(id);
	}

	public void addUrlToQueue(final String url) {
		if(!preExistingFulfillmentURLs.contains(url)) {
			urlQueue.add(url);
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().urlsToParseValue.setText(Integer.toString(urlQueue.size())));
		} else {
			System.out.println("We already have the fulfillment URL " + url + " in the DB... Skipping...");
		}
	}

	public void updateStatus(final String status) {
		this.publish(status);
	}

	@Override
	protected void process(List<String> chunks) {
		if(!chunks.isEmpty()) {
			final String status = chunks.get(chunks.size() - 1);
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText(status));
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		while(true) {
			if(!urlQueue.isEmpty()) {
				final Listing listing = FulfillmentParsingManager.parseListing(urlQueue.peek());
				if(listing != null) {
					attempts++;
					listing.url = urlQueue.peek();
					if(!listing.canShip) {
						System.out.println("Can't ship listing " + listing.title + "!");
					} else if(!platformToFulfillmentIds.getOrDefault(listing.fulfillmentPlatformId, new HashSet<>()).contains(listing.itemId)) {
						final boolean shouldDisplayListing = ListingQueue.isEmpty();
						ListingQueue.add(listing);
						if(shouldDisplayListing) {
							ListingToolGUI.getController().displayNextListing();
						}
						SwingUtilities.invokeLater(() -> ListingToolGUI.get().urlsToParseValue.setText(Integer.toString(urlQueue.size())));
					} else {
						System.out.println("We already have a mapping for item id " + listing.itemId + " on fulfillment platform " + listing.fulfillmentPlatformId + " in the DB");
					}
					urlQueue.poll();
					attempts = 0;
				} else if(attempts == LISTING_ATTEMPT_THRESHOLD) {
					System.out.println("Failed to parse URL: " + urlQueue.poll());
					attempts = 0;
				}
			}
			Thread.sleep(CYCLE_TIME);
		}
	}

	private void loadPreExistingFulfillmentURLs() {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT fulfillment_platform_id,item_id,listing_url FROM fulfillment_listing");
			while(res.next()) {
				preExistingFulfillmentURLs.add(res.getString("listing_url"));
				final Set<String> ids = platformToFulfillmentIds.getOrDefault(res.getString("fulfillment_platform_id"), new HashSet<>());
				ids.add(res.getString("item_id"));
				platformToFulfillmentIds.put(res.getInt("fulfillment_platform_id"), ids);
			}
			System.out.println("Loaded " + preExistingFulfillmentURLs.size() + " pre existing fulfillment URLs");
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
