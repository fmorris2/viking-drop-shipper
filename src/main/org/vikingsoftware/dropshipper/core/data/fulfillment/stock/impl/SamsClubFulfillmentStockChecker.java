package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jsoup.Jsoup;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuInventoryEntry;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.utils.SamsClubMetaDataParser;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class SamsClubFulfillmentStockChecker extends AbstractFulfillmentStockChecker<SamsClubWebDriver> {

	private static final String SEARCH_BASE_URL = "https://www.samsclub.com/s/";
	
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
	
	@Override
	protected Collection<SkuInventoryEntry> getStockImpl(MarketplaceListing marketListing, FulfillmentListing fulfillmentListing) {
		final Collection<SkuInventoryEntry> entries = new ArrayList<>();
		parseAndAddSkuInventoryEntries(marketListing, fulfillmentListing, entries);
		return entries;
	}
	
	protected void parseAndAddSkuInventoryEntries(MarketplaceListing marketListing,
			FulfillmentListing fulfillmentListing, Collection<SkuInventoryEntry> entries) {
		final List<SkuMapping> mappings = SkuMappingManager.getMappingsForMarketplaceListing(marketListing.id);
		System.out.println("SKU mappings for marketplace listing " + marketListing.id + ": " + mappings.size());
		try {
			final SamsClubMetaDataParser metaData = new SamsClubMetaDataParser();
			final String pageSource = Jsoup.connect(SEARCH_BASE_URL + fulfillmentListing.item_id).get().html();
			final boolean successfulParse = metaData.parse(pageSource);
			
			if(!successfulParse) {
				System.out.println("FAILED TO PARSE METADATA FOR fulfillmentListingItemId: " + fulfillmentListing.item_id);
				entries.add(new SkuInventoryEntry(null, -1, -1));
			}
			
			entries.add(new SkuInventoryEntry(null, parseItemStock(metaData), metaData.getPrice()));
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private int parseItemStock(final SamsClubMetaDataParser metaData) {		
		//TODO ENSURE TITLE MATCHES EXPECTED FULFILLMENT LISTING TITLE
		if(!metaData.passesAllListingConditions()) {
			System.out.println("Sams Club listing does not pass all listing conditions. Setting stock to 0.");
			return 0;
		}
		
		return metaData.getStock();
	}

	@Override
	protected int parseItemStock(final SamsClubWebDriver driver) { return -1; }

	@Override
	protected Class<? extends DriverSupplier<?>> getDriverSupplierClass() {
		return SamsClubDriverSupplier.class;
	}

	@Override
	protected double parseItemPrice(SamsClubWebDriver driver) { return -1.0; }
}
