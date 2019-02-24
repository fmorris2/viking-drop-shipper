package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.StateUtils;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

public class AliExpressOrderExecutionStrategy implements OrderExecutionStrategy {
	
	private static final String CREDS_FILE_PATH = "/data/aliexpress-creds.secure";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	
	private String username;
	private String password;
	
	public AliExpressOrderExecutionStrategy() {
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
	public List<Boolean> order(final List<CustomerOrder> orders) {
		return order(orders, false);
	}
	
	public List<Boolean> testOrder(final List<CustomerOrder> orders) {
		return order(orders, true);
	}
	
	private List<Boolean> order(final List<CustomerOrder> orders, final boolean isTest) {
		final List<Boolean> results = new ArrayList<>();
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(false);
		
		try {
			final WebDriver browser = new ChromeDriver(options);
			browser.manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			
			//sign in to ali express
			browser.get("https://login.aliexpress.com/");
			browser.switchTo().frame(browser.findElement(By.id("alibaba-login-box")));
			browser.findElement(By.id("fm-login-id")).sendKeys(username);
			browser.findElement(By.id("fm-login-password")).sendKeys(password);
			browser.findElement(By.id("fm-login-submit")).click();
			
			//wait for home page to appear
			browser.findElement(By.className("nav-user-account"));
			
			for(final CustomerOrder order : orders) {
				try {
					final List<FulfillmentListing> listings = FulfillmentManager.getListingsForOrder(order);
					
					if(listings.isEmpty()) {
						System.out.println("There are no FulfillmentListings present for customer order id " + order.id);
						results.add(false);
					}
					
					final boolean success = false;
					for(final FulfillmentListing listing : listings) {
						//navigate to fulfillment listing page
						browser.get(listing.listing_url);
						
						//select appropriate order options (color, length, etc)
						System.out.println("selecting order options...");
						if(!selectOrderOptions(browser, order, listing)) {
							continue;
						}
						
						browser.findElement(By.id("j-buy-now-btn")).click();
						
						System.out.println("entering shipping address...");
						if(!enterShippingAddress(browser, order, listing)) {
							continue;
						}
						
						System.out.println("saving shipping address...");
						browser.findElement(By.className("sa-confirm")).click();
						
						System.out.println("refreshing page...");
						browser.navigate().refresh();
						
						System.out.println("verifying shipping details...");
						if(!verifyShippingDetails(browser, order, listing)) {
							continue;
						}
						
						System.out.println("selecting payment method...");
					}
					
					results.add(success);
				} catch(final Exception e) {
					results.add(false);
					e.printStackTrace();
				}
			}
		} finally {
			//browser.close();
		}
		
		return results;
	}
	
	private static boolean verifyShippingDetails(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		
		System.out.println("verifying buyer name...");
		if(!browser.findElement(By.className("sa-username")).getText().equalsIgnoreCase(order.buyer_name)) {
			return false;
		}
		
		final WebElement locationBlock = browser.findElement(By.className("sa-location"));
		final List<String> locItems = locationBlock.findElements(By.tagName("li"))
				.stream()
				.map(el -> el.getText())
				.collect(Collectors.toList());
		
		System.out.println("verifying location details...");
		int locMatches = 0;
		for(final String locItem : locItems) {
			if(locItem.contains(order.buyer_street_address)) {
				locMatches++;
			}
			
			if(locItem.contains(order.buyer_apt_suite_unit_etc)) {
				locMatches++;
			}
			
			if(locItem.contains(order.buyer_city)) {
				locMatches++;
			}
			
			if(locItem.contains(order.buyer_state_province_region)) {
				locMatches++;
			}
			
			if(locItem.contains(StateUtils.getStateNameFromCode(order.buyer_state_province_region))) {
				locMatches++;
			}
			
			if(locItem.contains(order.buyer_zip_postal_code)) {
				locMatches++;
			}
			
			if(locItem.contains(order.buyer_country)) {
				locMatches++;
			}
		}
		
		return locMatches >= 6;	
	}
	
	private static boolean enterShippingAddress(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		System.out.println("Clicking edit address...");
		browser.findElement(By.className("sa-edit")).click();
		
		System.out.println("Selecting country...");
		final Select country = new Select(browser.findElement(By.className("sa-country")));
		country.selectByVisibleText(order.buyer_country);
		
		System.out.println("Selecting state / prov / region...");
		final Select stateProvRegion = new Select(browser.findElement(By.xpath("//*[@id=\"address-main\"]/div[1]/div[4]/div/select")));
		final String fullState = order.buyer_state_province_region.length() == 2 ? StateUtils.getStateNameFromCode(order.buyer_state_province_region)
				: order.buyer_state_province_region;
		stateProvRegion.selectByVisibleText(fullState);
		
		System.out.println("Selecting city...");
		final Select city = new Select(browser.findElement(By.xpath("//*[@id=\"address-main\"]/div[1]/div[5]/div/select")));
		city.selectByVisibleText(order.buyer_city);
		
		System.out.println("Iterating through inputs");
		final List<WebElement> inputs = browser.findElements(By.tagName("input"));
		System.out.println("inputs size: " + inputs.size());
		for(final WebElement input : inputs) { 
			switch(input.getAttribute("name").toLowerCase()) {
				case "contactperson":
					System.out.println("Setting contact name...");
					clearAndTypeInElement(input, order.buyer_name);
				break;
				case "address":
					System.out.println("Setting street address...");
					clearAndTypeInElement(input, order.buyer_street_address);
				break;
				case "address2":
					System.out.println("Setting apt / suite / unit / etc...");
					clearAndTypeInElement(input, order.buyer_apt_suite_unit_etc);
				break;
				case "zip":
					System.out.println("Setting zip code...");
					clearAndTypeInElement(input, order.buyer_zip_postal_code);
					if(order.buyer_zip_postal_code == null || order.buyer_zip_postal_code.isEmpty()) {
						System.out.println("Checking 'my address does not have a ZIP code' box...");
						final WebElement zipCheckBox = browser.findElement(By.xpath("//*[@id=\"address-main\"]/div[1]/div[6]/div/label/input"));
						if(!zipCheckBox.isSelected()) {
							zipCheckBox.click();
						}
						if(!zipCheckBox.isSelected()) {
							return false;
						}
					}
				break;
				
			}
		}
		
		return true;
	}
	
	private static void clearAndTypeInElement(final WebElement element, final String str) {
		element.clear();
		element.sendKeys(str);
	}
	
	private static boolean selectOrderOptions(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		try {
			//quantity input
			if(order.quantity > 1) {
				final WebElement quantityInput = browser.findElement(By.id("j-p-quantity-input"));
				if(quantityInput == null) {
					return false;
				}
				
				quantityInput.sendKeys(Keys.BACK_SPACE);
				quantityInput.sendKeys(Integer.toString(order.quantity));
			}
			
			final WebElement productInfoDiv = browser.findElement(By.id("j-product-info-sku"));
			final List<WebElement> propertyItems = productInfoDiv == null ? null : productInfoDiv.findElements(By.className("p-property-item"));
			if(propertyItems != null && !propertyItems.isEmpty()) {
				final JSONParser parser = new JSONParser();
				final JSONObject jsonObj = order.item_options == null ? new JSONObject() : (JSONObject)parser.parse(order.item_options);
				if(jsonObj.isEmpty()) {
					return true;
				}
				options:
				for(final WebElement propertyItem : propertyItems) {
					final String itemTitle = propertyItem.findElement(By.className("p-item-title"))
							.getText()
							.replace(":", "");
					
					final String valueToSelect = jsonObj.get(itemTitle).toString();
					System.out.println("Selecting " + valueToSelect + " for item " + itemTitle);
					
					final WebElement valueList = propertyItem.findElement(By.className("sku-attr-list"));
					final List<WebElement> listElements = valueList.findElements(By.xpath(".//li"));
					for(final WebElement listElement : listElements) {
						if(isMatchingOrderOption(listElement, valueToSelect)) {
							System.out.println("Clicking " + valueToSelect + " option for " + itemTitle);
							listElement.click();
							continue options;
						}
					}
					
					return false;
				}
			}
		} catch(final Exception e) {
			e.printStackTrace();
			System.out.println("Failed to select order options");
			return false;
		}
		
		return true;
	}
	
	private static boolean isMatchingOrderOption(final WebElement listElement, final String valueToSelect) {
		final WebElement dataRole = listElement.findElement(By.xpath(".//a"));
		if(dataRole == null) {
			return false;
		}
		else if(listElement.getAttribute("class").equals("item-sku-image")) { //image list item
			System.out.println("Dealing with an image list item!");
			if(dataRole != null && dataRole.getAttribute("title").equalsIgnoreCase(valueToSelect)) {
				System.out.println("Found matching image list item for " + valueToSelect);
				return true;
			}
		} else { //normal list item
			System.out.println("Dealing with a normal list item!");
			final WebElement span = dataRole.findElement(By.xpath(".//span"));
			if(span != null && span.getText().equals(valueToSelect)) {
				System.out.println("Found matching normal list item for " + valueToSelect);
				return true;
			}
		}
		
		return false;
	}
}
