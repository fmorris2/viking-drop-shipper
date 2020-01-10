package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.utils.ListingUtils;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsProductAPI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.AbstractFulfillmentParser;

public class SamsClubFulfillmentParser extends AbstractFulfillmentParser<SamsClubWebDriver> {
	
	private static final String PRODUCT_ID_REGEX = "\\/(prod)*(\\d{5,})";
	private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile(PRODUCT_ID_REGEX);
	private static final int MAX_LISTING_IMAGES = 8;

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
			final String productId = parseProductIdFromListingUrl(url);
			final SamsProductAPI api = new SamsProductAPI();
			System.out.println("Product ID: " + productId);
			api.parse(productId);
			
			final Listing listing = new Listing();
			listing.fulfillmentPlatformId = FulfillmentPlatforms.SAMS_CLUB.getId();
			listing.shippingService = ShippingServiceCodeType.SHIPPING_METHOD_STANDARD;
			
			if(api.hasVariations()) {
				System.out.println("\tListing has multiple variations. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(!api.isAvailableOnline()) {
				System.out.println("\tListing is not available online. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(!api.isFreeShipping()) {
				System.out.println("\tListing is not free shipping. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(api.hasMinPurchaseQty()) {
				System.out.println("\tListing has a min purchase quantity of > 1. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			listing.canShip = true;
			
			listing.ean = api.getEAN().orElse(null);
			listing.upc = api.getUPC().orElse(null);
			
			System.out.println("EAN: " + listing.ean);
			System.out.println("UPC: " + listing.upc);
			
			listing.title = api.getProductName().orElse(null);
			System.out.println("Product Name: " + listing.title);
			
			listing.itemId = api.getItemNumber().orElse(null);
			System.out.println("Item ID: " + listing.itemId);
			
			listing.productId = productId;
			System.out.println("Product ID: " + listing.productId);
			
			listing.description = api.getDescription().orElse("");
			
			api.getSpecifications().ifPresent(specs -> listing.description += "<br /><br />" + specs);
			
			ListingUtils.makeDescriptionPretty(listing);
			System.out.println("Description: " + listing.description);
			
			listing.price = api.getListPrice().orElse(-1D);
			if(listing.price < 0) {
				System.out.println("\tCould not parse listing price. Skipping...");
				listing.canShip = false;
				return listing;
			}
			System.out.println("Price: " + listing.price);
			
			listing.brand = api.getBrandName().orElse(null);
			System.out.println("Brand: " + listing.brand);
			
			listing.pictures = api.getImages();
			
			listing.mpn = api.getModelNumber().orElse(null);
			
			if(listing.pictures.size() > MAX_LISTING_IMAGES) {
				listing.pictures = listing.pictures.subList(0, MAX_LISTING_IMAGES);
			}
			System.out.println("Images loaded: " + listing.pictures.size());
			
			listing.propertyItems = Collections.emptyList();
			listing.variations = Collections.emptyMap();
			return listing;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String parseProductIdFromListingUrl(final String url) {
		final Matcher matcher = PRODUCT_ID_PATTERN.matcher(url);
		if(matcher.find()) {
			return (matcher.group(1) != null ? matcher.group(1) : "") + matcher.group(2);
		}
		
		return null;
	}

}
