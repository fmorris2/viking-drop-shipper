package main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl;

import java.util.ArrayList;
import java.util.Collection;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.AbstractFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsProductAPI;

public class SamsClubFulfillmentStockChecker extends AbstractFulfillmentStockChecker<SamsClubWebDriver> {

	private static final SamsProductAPI api = new SamsProductAPI();
	
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
	protected Collection<Pair<Integer,Double>> getStockImpl(MarketplaceListing marketListing, FulfillmentListing fulfillmentListing) {
		final Collection<Pair<Integer,Double>> entries = new ArrayList<>();
		parseAndAddSkuInventoryEntries(marketListing, fulfillmentListing, entries);
		return entries;
	}
	
	protected void parseAndAddSkuInventoryEntries(MarketplaceListing marketListing,
			FulfillmentListing fulfillmentListing, Collection<Pair<Integer,Double>> entries) {
		try {
			final int stock = parseItemStock(fulfillmentListing);
			entries.add(new Pair<>(stock, parseItemPrice(fulfillmentListing)));
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private int parseItemStock(final FulfillmentListing listing) {
		try {
			api.parse(listing.product_id);
			
			if(!listing.item_id.equalsIgnoreCase(api.getItemNumber().orElse(null))) {
				System.out.println("Could not parse metadata as the item IDs don't match!");
				return 0;
			}
			
			//TODO ENSURE TITLE MATCHES EXPECTED FULFILLMENT LISTING TITLE
			if(!api.passesAllListingConditions()) {
				System.out.println("Sams Club listing does not pass all listing conditions. Setting stock to 0.");
				return 0;
			}
			
			System.out.println("QTY FROM API: " + api.getAvailableToSellQuantity().orElse(0));
			return api.getAvailableToSellQuantity().orElse(0);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	private double parseItemPrice(final FulfillmentListing listing) {
		try {
			api.parse(listing.product_id);
			
			if(!listing.item_id.equalsIgnoreCase(api.getItemNumber().orElse(null))) {
				System.out.println("Could not parse metadata as the item IDs don't match!");
				return -1;
			}
			
			return api.getFinalPrice().orElse(-1D);
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
