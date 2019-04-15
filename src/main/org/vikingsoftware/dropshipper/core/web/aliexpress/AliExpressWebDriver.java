package main.org.vikingsoftware.dropshipper.core.web.aliexpress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;


public class AliExpressWebDriver extends LoginWebDriver {

	private final Map<String,String> cachedOrderOptions = new HashMap<>();

	@Override
	public boolean prepareForExecutionViaLoginImpl() {
		try {
			manage().window().maximize();
			manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);

			//sign in to ali express
			get("https://login.aliexpress.com/");
			switchTo().frame(findElement(By.id("alibaba-login-box")));

			findElement(By.id("fm-login-id")).sendKeys(account.username);
			findElement(By.id("fm-login-password")).sendKeys(account.password);
			findElement(By.id("fm-login-submit")).click();
			return true;
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to prepare for execution: ", e);
		}

		return false;
	}

	@Override
	protected String getLandingPageURL() {
		return "https://www.aliexpress.com";
	}

	@Override
	protected boolean verifyLoggedIn() {
		try {
			//wait for home page to appear
			findElement(By.className("nav-user-account"));
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void clearCachedSelectedOrderOptions() {
		cachedOrderOptions.clear();
	}

	public boolean selectOrderOptions(final SkuMapping skuMapping, final FulfillmentListing listing) {
		try {
			System.out.println("Selecting item options for sku mapping: " + skuMapping);
			final WebElement productInfoDiv = findElement(By.id("j-product-info-sku"));
			final List<WebElement> propertyItems = productInfoDiv == null ? null : productInfoDiv.findElements(By.className("p-property-item"));
			if(propertyItems != null && !propertyItems.isEmpty()) {
				final JSONParser parser = new JSONParser();

				final JSONObject jsonObj = (JSONObject)parser.parse(skuMapping.options);
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
					if(cachedOrderOptions.containsKey(itemTitle) && cachedOrderOptions.get(itemTitle).equalsIgnoreCase(valueToSelect)) {
						System.out.println(valueToSelect + " is already selected! Continuing...");
						continue;
					}

					System.out.println("Selecting " + valueToSelect + " for item " + itemTitle);

					final WebElement valueList = propertyItem.findElement(By.className("sku-attr-list"));
					final List<WebElement> listElements = valueList.findElements(By.xpath(".//li"));
					for(final WebElement listElement : listElements) {
						if(isMatchingOrderOption(listElement, valueToSelect)) {
							System.out.println("Clicking " + valueToSelect + " option for " + itemTitle);
							cachedOrderOptions.put(itemTitle, valueToSelect);
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

		if(dataRole != null && dataRole.getAttribute("data-sku-id").equalsIgnoreCase(valueToSelect)) {
			System.out.println("Found matching image list item for " + valueToSelect);
			return true;
		}

		return false;
	}

	public void scrollToBottomOfPage() {
		//scroll to bottom of page to load the descrip
		try {
			final Supplier<Integer> pageHeight = () -> Integer.parseInt(((JavascriptExecutor) this).executeScript("return document.body.scrollHeight").toString());
			final Supplier<Integer> currentHeight = () -> Integer.parseInt(((JavascriptExecutor) this).executeScript("return window.pageYOffset").toString());
			while(currentHeight.get() < pageHeight.get() * .85) {
				((JavascriptExecutor) this).executeScript("window.scrollBy(0, 300)", "");
				Thread.sleep(5);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
