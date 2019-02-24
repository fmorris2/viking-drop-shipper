package main.org.vikingsoftware.dropshipper.order.parser.strategy.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.stream.Collectors;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.order.parser.strategy.OrderParsingStrategy;

import org.json.simple.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EbayOrderParsingStrategy implements OrderParsingStrategy {
	
	private static final String CREDS_FILE_PATH = "/data/ebay-creds.secure";
	private static final String TRANSACTION_ID_PATTERN = "transId=([^&]*)";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	
	private String username;
	private String password;
	
	public EbayOrderParsingStrategy() {
		parseCredentials();
	}
	
	private void parseCredentials() {
		try(
				final InputStream inputStream = getClass().getResourceAsStream(CREDS_FILE_PATH);
				final InputStreamReader reader = new InputStreamReader(inputStream);
				final BufferedReader bR = new BufferedReader(reader);
			) {
				username = bR.readLine().trim();
				password = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Collection<CustomerOrder> parseNewOrders() {
		final Set<CustomerOrder> orders = new HashSet<>();
		
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(true);
		final WebDriver browser = new ChromeDriver(options);
		try {
			//obviously we need to sign in to ebay before we can do anything else
			browser.get("http://signin.ebay.com");
			browser.findElement(By.id("userid")).sendKeys(username);
			browser.findElement(By.id("pass")).sendKeys(password);
			browser.findElement(By.id("sgnBt")).click();
			
			//Navigate to "Sold Items" page, listing 200 items per page
			browser.get("https://my.ebay.com/ws/eBayISAPI.dll?MyEbayBeta&CurrentPage=MyeBayNextSold&ItemsPerPage=200");
			final List<WebElement> soldItems = browser.findElements(By.className("my_itl-itR")); //All sold item blocks
			if(!soldItems.isEmpty()) {
				//Convert all Sold Item WebElements to our CustomerOrder POJO (Plain Old Java Object)
				System.out.println("Found " + soldItems.size() + " sold items");
				final Collection<CustomerOrder> parsedOrders = parseSoldItems(browser, soldItems);
				if(parsedOrders != null) {
					orders.addAll(parsedOrders);
				}
			}
		} finally {
			browser.close();
		}
		
		return orders;
	}
	
	private Collection<CustomerOrder> parseSoldItems(final WebDriver browser, final List<WebElement> soldItems) {
		
		final Collection<CustomerOrder> orders = new HashSet<>();
		final Collection<Supplier<CustomerOrder>> orderSuppliers = new ArrayList<>();
		
		/*
		 * we need to make sure we're only taking into account orders for which we have
		 * ebay listings in our DB. So if you have personal orders on the same account (which we shouldn't, but just in case)
		 * the system will ignore them. So this chunk of code populates all of our ebay listings from the database
		*/
		final Set<MarketplaceListing> ebayListings = Marketplaces.EBAY.getMarketplace().getMarketplaceListings();
		final Map<String, Integer> ebayListingsMap = new HashMap<>(); //maps ebay listing id ==> DB primary key id for listings table
		for(final MarketplaceListing listing : ebayListings) {
			ebayListingsMap.put(listing.listingId, listing.id);
		}
		
		/*
		 * This loop will not parse items directly - It will grab the order details link for each sold items block,
		 * and add it to a queue to be parsed. The reason for this, is because we can't simply visit the order details page
		 * for each item while iterating over all the sold items block on the main page. If we did, the sold item blocks
		 * would become "stale" WebElements, since they no longer exist, and would throw errors. 
		 */
		for(final WebElement soldItem : soldItems) { //time to iterate through each sold item web element and add it to the parsing QUEUE
			final WebElement listingId = soldItem.findElement(By.className("g-vxs")); //parse the listing id out of the sold item block
			if(listingId != null) { //clearly we can't continue if we weren't able to parse the listing id
				final String listingIdText = listingId.getText().replace("(", "").replace(")", ""); //strip parenthesis from listing id
				if(ebayListingsMap.keySet().contains(listingIdText)) { //verify listing id is contained in our DB
					System.out.println("Found listing id: " + listingIdText);
					
					/*
					 * We still need to click "More actions" on each sold item block, as we can then
					 * parse the transaction id from the "View order details" link. We also need to navigate
					 * to the order details page to parse out the rest of the order info.
					 */
					final WebElement moreActions = soldItem.findElement(By.id("mT"));
					if(moreActions == null) { //if we can't find the more actions button, we can't proceed.
						return null;
					}
					
					moreActions.findElement(By.className("cta-ct")).click(); //Open the more actions menu
					
					//We need to wait until the order details link is visible, and then grab the WebElement for it
					final WebElement orderDetailsLink = new WebDriverWait(browser, DEFAULT_VISIBILITY_WAIT_SECONDS)
						.until(ExpectedConditions.elementToBeClickable(By.linkText("View order details")));
					
					//Grab the actual link the "View order details" will redirect you to
					final String orderDetailsLinkString = orderDetailsLink.getAttribute("href");
					System.out.println("orderDetailsLink: " + orderDetailsLinkString);
					
					//Parse out the Transaction ID with some nice REGEEXXXXXX
					final Pattern regex = Pattern.compile(TRANSACTION_ID_PATTERN);
					final Matcher matcher = regex.matcher(orderDetailsLinkString);
					
					if(matcher.find()) { //We can only proceed if our regex matches....
						final String transactionId = matcher.group(1);
						final String buyerUsername = soldItem.findElement(By.className("mbg-nw")).getText();
						final int dbId = ebayListingsMap.get(listingIdText);
						
						final String customizationJson = parseCustomizationJson(soldItem);
						
						//Add a parse request to the queue, but don't actually parse it yet.
						final Supplier<CustomerOrder> order = () -> parseOrderFromDetails(browser, dbId,
								listingIdText, transactionId, orderDetailsLinkString, buyerUsername, customizationJson);
						orderSuppliers.add(order);
					}
				} else {
					System.out.println("Found unknown Ebay customer order => db marketplace listing mapping");
				}
			}
		}
		
		/*
		 * Loop through parsing queue and finish the parsing process for each saved parsing request
		 */
		for(final Supplier<CustomerOrder> orderSupplier : orderSuppliers) {
			final CustomerOrder order = orderSupplier.get();
			if(order != null) {
				orders.add(order);
			}
		}
		
		return orders;
	}
	
	private String parseCustomizationJson(final WebElement soldItem) {
		final List<String> detailKeys = soldItem.findElements(By.className("g-vxsB"))
				.stream()
				.filter(element -> !element.getText().isEmpty())
				.map(element -> element.getText())
				.collect(Collectors.toList());
		
		List<String> detailVals = soldItem.findElements(By.className("g-vxs"))
				.stream()
				.filter(element -> !element.getText().isEmpty())
				.map(element -> element.getText())
				.collect(Collectors.toList());
		
		detailVals = detailVals.subList(2, detailVals.size());
		
		final JSONObject customizationJson = new JSONObject();
		for(int i = 0; i < detailKeys.size(); i++) {
			final String detailKey = detailKeys.get(i);
			if(detailKey.startsWith("Available Quantity")) {
				break;
			}
			
			customizationJson.put(detailKey.replace(":", ""), detailVals.get(i));
		}
		
		System.out.println("customizationJson: " + customizationJson.toJSONString());
		return customizationJson.toJSONString();
	}
	
	private CustomerOrder parseOrderFromDetails(final WebDriver browser, final int listingDbID, final String listingIdText, 
			final String transactionId, final String orderDetailsLink, final String buyerUsername, final String customizationJson) {
		
		CustomerOrder order = null;
		/*
		 * Clearly we only need to parse the order if we haven't already done so in the past.
		 * This if statement checks the transaction ID against what we know is in our DB, to see if we've already parsed it.
		 */
		if(!Marketplaces.EBAY.getMarketplace().isOrderIdKnown(transactionId)) {
			
			System.out.println("Encountered unknown Ebay transaction ID: " + transactionId);		
			System.out.println("Buyer Username: " + buyerUsername);
			
			//navigate to the "Order Details" page
			browser.get(orderDetailsLink);
			
			//grab the parent item details element which contains all of the relevant info
			final WebElement itemDetailsElement = waitForElement(browser, By.xpath("//*[@id=\""+listingIdText+"_ItemDetails\"]/table"));			
			
			//parse order quantity
			final WebElement qty = itemDetailsElement.findElement(By.id(listingIdText + "_Quantity"));		
			final int quantity = qty == null ? 1 : Integer.parseInt(qty.getText());
			System.out.println("Order Quantity: " + quantity);
			
			//parse price
			final WebElement priceElement = itemDetailsElement.findElement(By.xpath("//*[@id=\""+listingIdText+"_Price\"]"));
			final Double price = priceElement == null ? null : Double.parseDouble(priceElement.getText().replace("$", ""));
			System.out.println("Order Price: " + price);
			if(price == null) {
				System.out.println("Could not parse price for transaction " + transactionId);
			}
			
			//parse buyer name
			final WebElement buyerNameEl = browser.findElement(By.id("shippingAddressName"));
			final String buyerName = buyerNameEl == null ? null : buyerNameEl.getText();
			System.out.println("Buyer Name: " + buyerName);
			
			//parse street address
			final WebElement streetAddressEl = browser.findElement(By.id("shippingAddressLine1"));
			final String streetAddress = streetAddressEl == null ? null : streetAddressEl.getText();
			System.out.println("Buyer Street Address: " + streetAddress);
			
			//parse apt / suite / unit / etc
			final WebElement shippingAddressLine2 = browser.findElement(By.id("shippingAddressLine2"));
			final String buyerAptSuiteUnitEtc = shippingAddressLine2 == null ? "" : shippingAddressLine2.getText();		
			System.out.println("Buyer Apt Suite Unit Etc: " + buyerAptSuiteUnitEtc);
			
			//parse city state zip parts
			final WebElement cityStateZipEl = browser.findElement(By.id("shippingAddressCityStateZip"));
			final String cityStateZip = cityStateZipEl == null ? null : cityStateZipEl.getText();
			final String[] cityStateZipParts = cityStateZip.split(" ");
			String zip = null, city = null, state = null;
			if(cityStateZipParts.length < 3) {
				System.out.println("Unknown city state zip format: " + cityStateZip);
			} else {		
				//parse zip from parts
				zip = cityStateZipParts[cityStateZipParts.length - 1];
				System.out.println("Buyer Zip Code: " + zip);
				
				//parse state from parts
				state = cityStateZipParts[cityStateZipParts.length - 2];
				System.out.println("Buyer State: " + state);
				
				//parse city from parts
				city = "";
				for(int i = 0; i < cityStateZipParts.length - 2; i++) {
					city += cityStateZipParts[i];
					city += " ";
				}
				city = city.trim();		
				System.out.println("Buyer City: " + city);
			}
			
			//parse country from parts
			final WebElement countryEl = browser.findElement(By.id("shippingAddressCountry"));
			final String country = countryEl == null ? null : countryEl.getText();
			
			System.out.println("Buyer Country: " + country);
			
			//build our CustomerOrder POJO from all parsed info
			order = new CustomerOrder.Builder()
				.marketplace_listing_id(listingDbID)
				.quantity(quantity)
				.item_options(customizationJson)
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
