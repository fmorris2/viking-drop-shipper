package main.org.vikingsoftware.dropshipper.listing.tool.logic.workers;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.ThreadUtils;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParsingManager;

public class FulfillmentListingParserWorker extends SwingWorker<Void, String> {

	private static final long CYCLE_TIME = 50;
	private static final double MAX_LISTING_PRICE = 75.00;
	
	private static final Set<String> preExistingFulfillmentURLs = new HashSet<>();
	private static final Map<Integer, Set<String>> platformToFulfillmentIds = new HashMap<>();
	private static final Map<Integer, Set<String>> platformToFulfillmentTitles = new HashMap<>();

	private static FulfillmentListingParserWorker instance;

	private final ExecutorService threadPool = Executors.newFixedThreadPool(ThreadUtils.NUM_THREADS);
	private final Queue<String> urlQueue = new ConcurrentLinkedQueue<>();
	private final Queue<Listing> completedListings = new ConcurrentLinkedQueue<>();
	
	private int urlsToParse = 0;

	private FulfillmentListingParserWorker() {
		loadPreExistingFulfillmentURLs();
		execute();
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
	
	public static boolean isPreExistingItemTitle(final int platform, final String title) {
		return platformToFulfillmentTitles.getOrDefault(platform, new HashSet<>()).contains(title);
	}

	public void addUrlToQueue(final String url) {
		if(!preExistingFulfillmentURLs.contains(url)) {
			urlQueue.add(url);
			urlsToParse++;
			updateUrlsToParse();
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
			try {
				if(!urlQueue.isEmpty()) {
					final String url = urlQueue.poll();
					threadPool.execute(() -> parse(url));
				}
				
				while(!completedListings.isEmpty()) {
					handleCompletedListing(completedListings.poll());
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(CYCLE_TIME);
		}
	}
	
	private void parse(final String url) {
		try {
			System.out.println("Attempting to parse listing for " + url);
			final Listing listing = FulfillmentParsingManager.parseListing(url);
			if(listing != null) {
				System.out.println("Adding completed listing: " + listing);
				completedListings.add(listing);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		urlsToParse--;
		updateUrlsToParse();
	}
	
	private void handleCompletedListing(final Listing listing) {
		System.out.println("Successfully parsed listing for " + listing.url);
		if(isPreExistingItemId(listing.fulfillmentPlatformId, listing.itemId)) {
			System.out.println("We already have item id " + listing.itemId + " in our DB. Skipping...");
		} else if(isPreExistingItemTitle(listing.fulfillmentPlatformId, listing.title)) {
			System.out.println("We already have item title " + listing.title + " in our DB. Skipping...");
	    } else if(!listing.canShip) {
			System.out.println("Can't ship listing " + listing.url + "!");
		} else if(listing.price > MAX_LISTING_PRICE) {
			System.out.println("Listing price is over our comfortable threshold!");
		} else if(!platformToFulfillmentIds.getOrDefault(listing.fulfillmentPlatformId, new HashSet<>()).contains(listing.itemId)) {
			final boolean shouldDisplayListing = ListingQueue.isEmpty();
			ListingQueue.add(listing);
			if(shouldDisplayListing) {
				ListingToolGUI.getController().displayNextListing();
			}
			updateUrlsToParse();
		} else {
			System.out.println("We already have a mapping for item id " + listing.itemId + " on fulfillment platform " + listing.fulfillmentPlatformId + " in the DB");
		}
	}

	private void loadPreExistingFulfillmentURLs() {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT fulfillment_platform_id,item_id,listing_url,listing_title FROM fulfillment_listing");
			while(res.next()) {
				preExistingFulfillmentURLs.add(res.getString("listing_url"));
				final Set<String> ids = platformToFulfillmentIds.getOrDefault(res.getInt("fulfillment_platform_id"), new HashSet<>());
				ids.add(res.getString("item_id"));
				platformToFulfillmentIds.put(res.getInt("fulfillment_platform_id"), ids);
				
				final Set<String> titles = platformToFulfillmentTitles.getOrDefault(res.getInt("fulfillment_platform_id"), new HashSet<>());
				titles.add(res.getString("listing_title"));
				platformToFulfillmentTitles.put(res.getInt("fulfillment_platform_id"), titles);
			}
			System.out.println("Loaded " + preExistingFulfillmentURLs.size() + " pre existing fulfillment URLs");
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updateUrlsToParse() {
		SwingUtilities.invokeLater(() -> ListingToolGUI.get().urlsToParseValue.setText(Integer.toString(urlsToParse)));
	}
}
