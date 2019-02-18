package main.org.vikingsoftware.dropshipper.order.parser.strategy.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EbayOrderParsingStrategy implements OrderParsingStrategy {
	
	private static final String TRANSACTION_ID_PATTERN = "transId=([^&]*)";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	
	@Override
	public Collection<CustomerOrder> parseNewOrders() {
		return null;
	}
	
	public Collection<CustomerOrder> parseOrders() {
		final Set<CustomerOrder> orders = new HashSet<>();
		
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(true);
		final WebDriver browser = new ChromeDriver(options);
		try {
			browser.get("http://signin.ebay.com");
			browser.findElement(By.id("userid")).sendKeys("bmrosa");
			browser.findElement(By.id("pass")).sendKeys("ewg-YH9-xEG-Y9S");
			browser.findElement(By.id("sgnBt")).click();
			browser.get("https://my.ebay.com/ws/eBayISAPI.dll?MyEbayBeta&CurrentPage=MyeBayNextSold&ItemsPerPage=200");
			final List<WebElement> soldItems = browser.findElements(By.className("my_itl-itR"));
			if(!soldItems.isEmpty()) {
				orders.addAll(parseSoldItems(browser, soldItems));
			}
		} finally {
			browser.close();
		}
		
		return orders;
	}
	
	private Collection<CustomerOrder> parseSoldItems(final WebDriver browser, final List<WebElement> soldItems) {
		final Collection<CustomerOrder> orders = new HashSet<>();
		final Set<MarketplaceListing> ebayListings = Marketplaces.EBAY.getMarketplace().getMarketplaceListings();
		final Map<String, Integer> ebayListingsMap = new HashMap<>();
		for(final MarketplaceListing listing : ebayListings) {
			ebayListingsMap.put(listing.listingId, listing.id);
		}
		
		final Collection<Supplier<CustomerOrder>> orderSuppliers = new ArrayList<>();
		
		System.out.println("Found " + soldItems.size() + " sold items");
		for(final WebElement soldItem : soldItems) {
			final WebElement listingId = soldItem.findElement(By.className("g-vxs"));
			if(listingId != null) {
				final String listingIdText = listingId.getText().replace("(", "").replace(")", "");
				//if(ebayListingIds.keySet().contains(listingIdText)) {
					System.out.println("Found listing id: " + listingIdText);
					
					final WebElement moreActions = soldItem.findElement(By.id("mT"));
					if(moreActions == null) {
						return null;
					}
					
					moreActions.findElement(By.className("cta-ct")).click();
					
					final WebElement orderDetailsLink = new WebDriverWait(browser, DEFAULT_VISIBILITY_WAIT_SECONDS)
						.until(ExpectedConditions.elementToBeClickable(By.linkText("View order details")));
					
					final String orderDetailsLinkString = orderDetailsLink.getAttribute("href");
					System.out.println("orderDetailsLink: " + orderDetailsLinkString);
					final Pattern regex = Pattern.compile(TRANSACTION_ID_PATTERN);
					final Matcher matcher = regex.matcher(orderDetailsLinkString);
					
					if(matcher.find()) {
						final String transactionId = matcher.group(1);
						final String buyerUsername = soldItem.findElement(By.className("mbg-nw")).getText();
						final int listingDbId = ebayListingsMap.getOrDefault(listingIdText, -1);
						if(listingDbId != -1) {
							final Supplier<CustomerOrder> order = () -> parseOrderFromDetails(browser, listingDbId,
									listingIdText, transactionId, orderDetailsLinkString, buyerUsername);
							orderSuppliers.add(order);
						} else {
							System.out.println("Found unknown Ebay customer order => db marketplace listing mapping");
						}
					}
				//}
			}
		}
		
		for(final Supplier<CustomerOrder> orderSupplier : orderSuppliers) {
			final CustomerOrder order = orderSupplier.get();
			if(order != null) {
				orders.add(order);
			}
		}
		
		return orders;
	}
	
	private CustomerOrder parseOrderFromDetails(final WebDriver browser, final int listingDbID, final String listingIdText, 
			final String transactionId, final String orderDetailsLink, final String buyerUsername) {
		
		CustomerOrder order = null;
		if(!Marketplaces.EBAY.getMarketplace().isOrderIdKnown(transactionId)) {
			System.out.println("Encountered unknown Ebay transaction ID: " + transactionId);
			
			System.out.println("Buyer Username: " + buyerUsername);
			
			browser.get(orderDetailsLink);
			
			final WebElement itemDetailsElement = waitForElement(browser, By.xpath("//*[@id=\""+listingIdText+"_ItemDetails\"]/table"));
			
			final WebElement qty = itemDetailsElement.findElement(By.id(listingIdText + "_Quantity"));
			final int quantity = qty == null ? 1 : Integer.parseInt(qty.getText());
			System.out.println("Order Quantity: " + quantity);
			final WebElement priceElement = itemDetailsElement.findElement(By.xpath("//*[@id=\""+listingIdText+"_Price\"]"));
			final double price = priceElement == null ? -1 : Double.parseDouble(priceElement.getText().replace("$", ""));
			System.out.println("Order Price: " + price);
			if(price == -1) {
				System.out.println("Could not parse price for transaction " + transactionId);
				return null;
			}
			
			final String buyerName = browser.findElement(By.id("shippingAddressName")).getText();
			System.out.println("Buyer Name: " + buyerName);
			final String streetAddress = browser.findElement(By.id("shippingAddressLine1")).getText();
			System.out.println("Buyer Street Address: " + streetAddress);
			final WebElement shippingAddressLine2 = browser.findElement(By.id("shippingAddressLine2"));
			final String buyerAptSuiteUnitEtc = shippingAddressLine2 == null ? "" : shippingAddressLine2.getText();
			System.out.println("Buyer Apt Suite Unit Etc: " + buyerAptSuiteUnitEtc);
			final String cityStateZip = browser.findElement(By.id("shippingAddressCityStateZip")).getText();
			final String[] cityStateZipParts = cityStateZip.split(" ");
			if(cityStateZipParts.length < 3) {
				System.out.println("Unknown city state zip format: " + cityStateZip);
				return null;
			}
			final String zip = cityStateZipParts[cityStateZipParts.length - 1];
			System.out.println("Buyer Zip Code: " + zip);
			final String state = cityStateZipParts[cityStateZipParts.length - 2];
			System.out.println("Buyer State: " + state);
			String city = "";
			for(int i = 0; i < cityStateZipParts.length - 2; i++) {
				city += cityStateZipParts[i];
				city += " ";
			}
			city = city.trim();
			
			System.out.println("Buyer City: " + city);
			
			final String country = browser.findElement(By.id("shippingAddressCountry")).getText();
			System.out.println("Buyer Country: " + country);
			
			order = new CustomerOrder.Builder()
				.marketplace_listing_id(listingDbID)
				.quantity(quantity)
				.marketplace_order_id(transactionId)
				.buyer_username(buyerUsername)
				.buyer_name(buyerName)
				.buyer_country(country)
				.buyer_street_address(streetAddress)
				.buyer_apt_suite_unit_etc(buyerAptSuiteUnitEtc)
				.buyer_state_province_region(state)
				.buyer_city(city)
				.buyer_zip_postal_code(zip)
				.build();
		} else {
			System.out.println("We already know Ebay transaction ID " + transactionId);
		}
		
		return order;
	}
	
	private WebElement waitForElement(final WebDriver browser, final By selector) {
		return new WebDriverWait(browser, DEFAULT_VISIBILITY_WAIT_SECONDS).until(ExpectedConditions.presenceOfElementLocated(selector));
	}

}
