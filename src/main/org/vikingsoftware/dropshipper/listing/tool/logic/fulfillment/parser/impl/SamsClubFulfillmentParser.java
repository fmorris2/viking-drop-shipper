package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubFulfillmentStockChecker;
import main.org.vikingsoftware.dropshipper.core.utils.ListingUtils;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubProductAPI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParser;

public class SamsClubFulfillmentParser implements FulfillmentParser {
	
	private static final String PRODUCT_ID_REGEX = "\\/(prod)*(\\d{5,})";
	private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile(PRODUCT_ID_REGEX);
	private static final int MAX_LISTING_IMAGES = 8;

	@Override
	public Listing getListingTemplate(final FulfillmentAccount account, final String url) {
		try {
			final String productId = parseProductIdFromListingUrl(url);
			final SamsClubProductAPI api = new SamsClubProductAPI();
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
			
			if(api.isFlowersTemplateProduct()) {
				System.out.println("\tListing is flowers product. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			if(api.isGiftCard()) {
				System.out.println("\tListing is gift card. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			listing.minPurchaseQty = api.getMinPurchaseQty();
			
			listing.canShip = true;
			
			listing.ean = api.getEAN().orElse(null);
			listing.upc = api.getUPC().orElse(null);
			listing.skuId = api.getSkuId().orElse(null);
			
			System.out.println("EAN: " + listing.ean);
			System.out.println("UPC: " + listing.upc);
			System.out.println("SKU ID: " + listing.skuId);
			
			listing.title = cleanse(api.getProductName().orElse(null)
					.replace(" and ", "&")
					.replace("with", "w/"));
			
			listing.itemId = api.getItemNumber().orElse(null);
			System.out.println("Item ID: " + listing.itemId);
			
			listing.productId = productId;
			System.out.println("Product ID: " + listing.productId);
			
			listing.description = cleanse(api.getDescription().orElse(""));
			
			api.getSpecifications().ifPresent(specs -> listing.description += "<br /><br />" + cleanse(specs));
			
			
			if(listing.minPurchaseQty > 1) {
				listing.title = "[" + listing.minPurchaseQty + "x] " + listing.title;
				listing.description = "<h3>ATTENTION: THIS IS A " + listing.minPurchaseQty + " PACK. "
						+ "THE PACKAGE CONTAINS " + listing.minPurchaseQty + " OF THIS ITEM.</h3><br /><hr />"
						+ listing.description;
			}
			
			System.out.println("Listing Title: " + listing.title);
			
			ListingUtils.makeDescriptionPretty(listing);
			System.out.println("Description: " + listing.description);
			
			listing.price = api.getListPrice().orElse(-1D) * listing.minPurchaseQty;
			if(listing.price <= 0) {
				System.out.println("\tCould not parse listing price. Skipping...");
				listing.canShip = false;
				return listing;
			}
			
			listing.price += SamsClubFulfillmentStockChecker.SAMS_CLUB_SHIPPING_RATE;
			
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
	
	private String cleanse(String text) {
		
		// Match all html tags with A or SCRIPT
		text = text.replaceAll("<(a|script)[\\s\\S]*?>[\\s\\S]*?<\\/(a|script)>", "");

		// Match all phone numbers
		text = text.replaceAll("(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}", "");

		// Match all URLs
		text = text.replaceAll("[^src\\=\"]https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)[^\"]", "");

		// Match Sam's Club, SquareTrade, Manufacturer, Customer Service, shipping
		text = text.replaceAll("(sam'?s\\s?club|warranty|square\\s?trade|allstate|manufacturer|customer\\s(service|support)|shipping|)", "");

		// Remove all ascii characters
		text = text.replaceAll("[^\\p{ASCII}]", "");
		
		return text;
	}
	
	private String parseProductIdFromListingUrl(final String url) {
		final Matcher matcher = PRODUCT_ID_PATTERN.matcher(url);
		if(matcher.find()) {
			return (matcher.group(1) != null ? matcher.group(1) : "") + matcher.group(2);
		}
		
		return null;
	}

}
