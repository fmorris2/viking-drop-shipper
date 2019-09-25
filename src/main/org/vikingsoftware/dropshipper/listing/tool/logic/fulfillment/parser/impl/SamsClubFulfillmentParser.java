package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.util.Collections;

import org.jsoup.Jsoup;

import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.utils.ListingUtils;
import main.org.vikingsoftware.dropshipper.core.utils.SamsClubMetaDataParser;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.AbstractFulfillmentParser;

public class SamsClubFulfillmentParser extends AbstractFulfillmentParser<SamsClubWebDriver> {

	@Override
	public Class<SamsClubDriverSupplier> getDriverSupplierClass() {
		return null;
	}

	@Override
	public boolean needsToLogin() {
		return false;
	}

	@Override
	protected Listing parseListing(final String url) {
		try {
			final SamsClubMetaDataParser metaDataParser = new SamsClubMetaDataParser();
			final Listing listing = new Listing();
			listing.fulfillmentPlatformId = FulfillmentPlatforms.SAMS_CLUB.getId();
			listing.shippingService = ShippingServiceCodeType.SHIPPING_METHOD_STANDARD;
			
			final String pageSource = Jsoup.connect(url).get().html();
			metaDataParser.parse(pageSource);
			
			if(metaDataParser.hasVariations()) {
				System.out.println("\tListing has multiple variations. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(!metaDataParser.isAvailableOnline()) {
				System.out.println("\tListing is not available online. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(!metaDataParser.isFreeShipping()) {
				System.out.println("\tListing is not free shipping. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(metaDataParser.hasMinPurchaseQty()) {
				System.out.println("\tListing has a min purchase quantity of > 1. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			final String searchUrl = SamsClubFulfillmentStockChecker.SEARCH_BASE_URL + metaDataParser.getItemID();
			if(!new SamsClubMetaDataParser().parse(searchUrl)) {
				System.out.println("\tListing fails Sams Club search test. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			listing.canShip = true;
			
			listing.title = metaDataParser.getProductName();
			System.out.println("Product Name: " + listing.title);
			
			listing.itemId = metaDataParser.getItemID();
			System.out.println("Item ID: " + listing.itemId);
			
			listing.description = metaDataParser.getDescription();
			ListingUtils.makeDescriptionPretty(listing);
			System.out.println("Description: " + listing.description);
			
			listing.price = metaDataParser.getPrice();
			System.out.println("Price: " + listing.price);
			
			listing.brand = metaDataParser.getBrand();
			System.out.println("Brand: " + listing.brand);
			
			listing.pictures = metaDataParser.getImages();
			System.out.println("Images loaded: " + listing.pictures.size());
			
			listing.propertyItems = Collections.emptyList();
			listing.variations = Collections.emptyMap();
			return listing;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
