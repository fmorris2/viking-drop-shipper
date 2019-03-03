package main.org.vikingsoftware.dropshipper.core.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;


public class AliExpressWebDriver extends LoginWebDriver {
	
	private static final String CREDS_FILE_PATH = "/data/aliexpress-creds.secure";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	private static final int MAX_LOGIN_TRIES = 20;
	
	private String username;
	private String password;
	private int loginTries = 0;
	
	public AliExpressWebDriver() {
		super();
		parseCredentials();
	}
	
	@Override
	public boolean getReady() {
		return prepareForExecution();
	}
	
	private boolean prepareForExecution() {
		try {
			manage().window().maximize();
			manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			
			//sign in to ali express
			get("https://login.aliexpress.com/");
			switchTo().frame(findElement(By.id("alibaba-login-box")));
			
			findElement(By.id("fm-login-id")).sendKeys(username);
			findElement(By.id("fm-login-password")).sendKeys(password);
			findElement(By.id("fm-login-submit")).click();

			try {
				//wait for home page to appear
				findElement(By.className("nav-user-account"));
			} catch(final NoSuchElementException e) {
				if(loginTries < MAX_LOGIN_TRIES) {
					System.out.println("encountered verification element... retrying.");
					loginTries++;
					get("http://www.google.com");
					return prepareForExecution();
				} else {
					return false;
				}
			}
			
			return true;
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to prepare for execution: ", e);
		}
		
		close();
		return false;
	}
	
	public boolean selectOrderOptions(final SkuMapping skuMapping, final FulfillmentListing listing) {
		try {
			System.out.println("Selecting item options for sku mapping: " + skuMapping);
			final WebElement productInfoDiv = findElement(By.id("j-product-info-sku"));
			final List<WebElement> propertyItems = productInfoDiv == null ? null : productInfoDiv.findElements(By.className("p-property-item"));
			if(propertyItems != null && !propertyItems.isEmpty()) {
				final JSONParser parser = new JSONParser();
				
				final JSONObject jsonObj = (JSONObject)parser.parse(skuMapping.ali_express_options);
				System.out.println("json obj: " + jsonObj);
				if(jsonObj.isEmpty()) {
					return true;
				}
				options:
				for(final WebElement propertyItem : propertyItems) {
					final String itemTitle = propertyItem.findElement(By.className("p-item-title"))
							.getText()
							.replace(":", "");
					
					if(!jsonObj.containsKey(itemTitle)) {
						continue;
					}
					
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
			DBLogging.high(getClass(), "Failed to select order options", e);
			return false;
		}
		
		return true;
	}
	
	private boolean isMatchingOrderOption(final WebElement listElement, final String valueToSelect) {
		final WebElement dataRole = listElement.findElement(By.xpath(".//a"));
		if(dataRole == null) {
			return false;
		}
		else if(listElement.getAttribute("class").contains("item-sku-image")) { //image list item
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
	
}
