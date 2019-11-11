package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.jsoup.Jsoup;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.utils.SamsClubMetaDataParser;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubFulfillmentStockChecker extends AbstractFulfillmentStockChecker<SamsClubWebDriver> {

	private static final SamsClubMetaDataParser metaData = new SamsClubMetaDataParser();
	
	private static SamsClubFulfillmentStockChecker instance;

	private SamsClubFulfillmentStockChecker() {
		super();
	}

	public synchronized static SamsClubFulfillmentStockChecker get() {
		if(instance == null) {
			instance = new SamsClubFulfillmentStockChecker();
		}

		return instance;
	}
	
	public static void main(final String[] args) throws IOException {
		final String pageSource = Jsoup.connect("https://www.samsclub.com/p/mm-fluticasone-6x120-sprays/prod21003064").get().html();
		final String itemId = "761285";
		final int stock = new SamsClubFulfillmentStockChecker().parseItemStock(itemId, pageSource);
		new SamsClubFulfillmentStockChecker().parseItemPrice(itemId, pageSource);
		System.out.println("Stock: " + stock);
	}
	
	@Override
	protected Collection<Pair<Integer,Double>> getStockImpl(MarketplaceListing marketListing, FulfillmentListing fulfillmentListing) {
		final Collection<Pair<Integer,Double>> entries = new ArrayList<>();
		parseAndAddSkuInventoryEntries(marketListing, fulfillmentListing, entries);
		return entries;
	}
	
	protected void parseAndAddSkuInventoryEntries(MarketplaceListing marketListing,
			FulfillmentListing fulfillmentListing, Collection<Pair<Integer,Double>> entries) {
		try {
			final String pageSource = Jsoup.connect(fulfillmentListing.listing_url).get().html();
			final int stock = parseItemStock(fulfillmentListing.item_id, pageSource);
			entries.add(new Pair<>(stock, parseItemPrice(fulfillmentListing.item_id, pageSource)));
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private int parseItemStock(final String itemId, final String pageSource) {
		try {
			metaData.parse(pageSource);
			
			if(!itemId.equalsIgnoreCase(metaData.getItemID())) {
				System.out.println("Could not parse metadata as the item IDs don't match!");
				return 0;
			}
			
			//TODO ENSURE TITLE MATCHES EXPECTED FULFILLMENT LISTING TITLE
			if(!metaData.passesAllListingConditions()) {
				System.out.println("Sams Club listing does not pass all listing conditions. Setting stock to 0.");
				return 0;
			}
			
			return metaData.getStock();
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	private double parseItemPrice(final String itemId, final String pageSource) {
		try {
			metaData.parse(pageSource);
			
			if(!itemId.equalsIgnoreCase(metaData.getItemID())) {
				System.out.println("Could not parse metadata as the item IDs don't match!");
				return -1;
			}
			
			return metaData.getPrice().orElse(-1D);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	}

	@Override
	protected int parseItemStock(final SamsClubWebDriver driver) { System.err.println("SHOULDN'T GET HERE!"); System.exit(0); return -1; }

	@Override
	protected Class<? extends DriverSupplier<?>> getDriverSupplierClass() {
		return SamsClubDriverSupplier.class;
	}

	@Override
	protected double parseItemPrice(SamsClubWebDriver driver) { return -1.0; }
}
