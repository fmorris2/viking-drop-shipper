package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class CostcoUtils {
	
	private static final String LIMIT_PER_MEMBER_PATTERN_STRING = "Limit (\\d+) per member";
	private static final Pattern LIMIT_PER_MEMBER_PATTERN = Pattern.compile(LIMIT_PER_MEMBER_PATTERN_STRING);
	
	private static final String LISTING_TITLE_PATTERN_STRING = "\"productName\" : \"(.+)\",";
	private static final Pattern LISTING_TITLE_PATTERN = Pattern.compile(LISTING_TITLE_PATTERN_STRING);
	
	private static final String PRODUCT_ID_PATTERN_STRING = "productId : '(.+)',";
	private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile(PRODUCT_ID_PATTERN_STRING);
	
	private static final String TWO_DAY_SHIPPING_TEXT = "Two day transit is included in the quoted price";
	
	private CostcoUtils() {
		//can't instantiate utils class
	}
	
	/**
	 * Given the page source of a Costco fulfillment listing,
	 * parse out any buy limits per member.
	 * 
	 * @param listingPageSource page source of the costco fulfillment listing
	 * @return the listing limit per member, or -1 if there is no limit
	 */
	public static int getListingLimitPerMember(final String listingPageSource) {
		final Matcher matcher = LIMIT_PER_MEMBER_PATTERN.matcher(listingPageSource);
		if(matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		
		return -1;
	}
	
	/**
	 * Given the page source of a Costco fulfillment listing,
	 * parse out whether or not the listing is 2-day shipping.
	 * 
	 * @param pageSource page source of the costco fulfillment listing
	 * @return true if the listing has 2-day shipping, false otherwise
	 */
	public static boolean isTwoDayShipping(final String pageSource) {
		final Document doc = Jsoup.parse(pageSource);
		final Elements shippingInfoElements = doc.getElementsByClass("product-info-shipping");
		for(final Element shippingInfo : shippingInfoElements) {
			if(shippingInfo.text().contains(TWO_DAY_SHIPPING_TEXT)) {
				System.out.println("Two day shipping detected.");
				return true;
			}
		}
		
		if(pageSource.contains("<img class=\"pdp-bad\" src=\"/wcsstore/CostcoGLOBALSAS/images/Grocery_Badge.svg\" alt=\"2-Day Delivery\">")) {
			System.out.println("Two day shipping detected.");
			return true;
		}
		
		return false;
	}
	
	public static String parseListingTitleFromPageSource(final String pageSource) {
		final Matcher matcher = LISTING_TITLE_PATTERN.matcher(pageSource);
		if(matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
	public static String getProductIdFromPageSource(final String pageSource) {
		final Matcher matcher = PRODUCT_ID_PATTERN.matcher(pageSource);
		if(matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
	
}
