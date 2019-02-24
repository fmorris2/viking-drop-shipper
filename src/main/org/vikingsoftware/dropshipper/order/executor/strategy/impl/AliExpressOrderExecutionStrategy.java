package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.misc.CreditCardInfo;
import main.org.vikingsoftware.dropshipper.core.data.misc.StateUtils;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.CaptchaUtils;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;

public class AliExpressOrderExecutionStrategy implements OrderExecutionStrategy {
	
	private static final String CREDS_FILE_PATH = "/data/aliexpress-creds.secure";
	private static final String CARD_INFO_PATH = "/data/aliexpress-payment-method.secure";
	private static final String ORDER_ID_PATTERN = "orderId=([^&]*)";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	private static final int ORDER_SUCCESS_WAIT_SECONDS = 60;
	private static final int MAX_LOGIN_TRIES = 20;
	
	private String username;
	private String password;
	private CreditCardInfo cardInfo;
	private WebDriver browser;
	
	private int loginTries = 0;
	
	public AliExpressOrderExecutionStrategy() {
		parseCredentials();
		parseCardInfo();
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
	
	private void parseCardInfo() {
		cardInfo = new CreditCardInfo();
		try(
				final InputStream inputStream = getClass().getResourceAsStream(CARD_INFO_PATH);
				final InputStreamReader reader = new InputStreamReader(inputStream);
				final BufferedReader bR = new BufferedReader(reader);
			) {
				cardInfo.cardNumber = bR.readLine().trim();
				cardInfo.expMonth = bR.readLine().trim();
				cardInfo.expYear = bR.readLine().trim();
				cardInfo.secCode = bR.readLine().trim();
				cardInfo.firstName = bR.readLine().trim();
				cardInfo.lastName = bR.readLine().trim();
				
				cardInfo.country = bR.readLine().trim();
				cardInfo.streetAddr = bR.readLine().trim();
				cardInfo.streetAddr2 = bR.readLine().trim();
				cardInfo.city = bR.readLine().trim();
				cardInfo.state = bR.readLine().trim();
				cardInfo.zip = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean prepareForExecution() {
		try {
			final ChromeOptions options = new ChromeOptions();
			options.setHeadless(false);
			
			browser = new ChromeDriver(options);
			
			browser.manage().window().maximize();
			browser.manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			
			//sign in to ali express
			browser.get("https://login.aliexpress.com/");
			browser.switchTo().frame(browser.findElement(By.id("alibaba-login-box")));
			
			browser.findElement(By.id("fm-login-id")).sendKeys(username);
			browser.findElement(By.id("fm-login-password")).sendKeys(password);
			browser.findElement(By.id("fm-login-submit")).click();

			try {
				//wait for home page to appear
				browser.findElement(By.className("nav-user-account"));
			} catch(final NoSuchElementException e) {
				if(loginTries < MAX_LOGIN_TRIES) {
					System.out.println("encountered verification element... retrying.");
					browser.close();
					loginTries++;
					return prepareForExecution();
				} else {
					return false;
				}
			}
			
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		browser.close();
		return false;
	}
	
	@Override
	public void finishExecution() {
		browser.close();
	}
	
	@Override
	public ProcessedOrder order(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		return order(order, fulfillmentListing, false);
	}
	
	public ProcessedOrder testOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		return order(order, fulfillmentListing, true);
	}
	
	private ProcessedOrder order(final CustomerOrder customerOrder, final FulfillmentListing fulfillmentListing, final boolean isTest) {
		final ProcessedOrder processedOrder = new ProcessedOrder(customerOrder.id, fulfillmentListing.id, null, "failed");		
			try {
				return executeOrder(customerOrder, fulfillmentListing, browser, isTest);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		
		return processedOrder;
	}
	
	private ProcessedOrder executeOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing, 
			final WebDriver browser, final boolean isTest) {
		
		final ProcessedOrder processedOrder = new ProcessedOrder(order.id, fulfillmentListing.id, null, "failed");
		//navigate to fulfillment listing page
		browser.get(fulfillmentListing.listing_url);
		
		//select appropriate order options (color, length, etc)
		System.out.println("selecting order options...");
		if(!selectOrderOptions(browser, order, fulfillmentListing)) {
			return processedOrder;
		}
		
		browser.findElement(By.id("j-buy-now-btn")).click();
		
		System.out.println("verifying price...");
		if(!verifyPrice(browser, order, fulfillmentListing)) {
			return processedOrder;
		}
		
		System.out.println("entering shipping address...");
		if(!enterShippingAddress(browser, order, fulfillmentListing)) {
			return processedOrder;
		}
		
		System.out.println("saving shipping address...");
		browser.findElement(By.className("sa-confirm")).click();
		
		System.out.println("refreshing page...");
		browser.navigate().refresh();
		
		System.out.println("verifying shipping details...");
		if(!verifyShippingDetails(browser, order, fulfillmentListing)) {
			return processedOrder;
		}
		
		System.out.println("selecting payment method...");
		if(!enterPaymentMethod(browser, order, fulfillmentListing)) {
			return processedOrder;
		}
		
		try {
			final WebElement captchaElement = browser.findElement(By.id("captcha-image"));
			if(captchaElement != null) {
				System.out.println("solving captcha...");
				final File screenshot = takeScreenshot(browser, captchaElement);
				final String solvedCaptcha = CaptchaUtils.solveSimpleCaptcha(screenshot);
				System.out.println("solvedCaptcha: " + solvedCaptcha);
			}
		} catch(final NoSuchElementException e) {
			System.out.println("there is no captcha to solve, moving forward...");
		}
		

		return isTest ? new ProcessedOrder(order.id, fulfillmentListing.id, "test", "failed")
					  : finalizeOrder(browser, order, fulfillmentListing);
	}
	
	private ProcessedOrder finalizeOrder(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		final ProcessedOrder failedOrder = new ProcessedOrder(order.id, listing.id, null, "failed");
		System.out.println("clicking confirm & pay...");
		browser.findElement(By.id("place-order-btn")).click();
		
		browser.manage().timeouts().implicitlyWait(ORDER_SUCCESS_WAIT_SECONDS, TimeUnit.SECONDS);
		
		try {
			final WebElement feedbackHeader = browser.findElement(By.xpath("/html/body/div[5]/div[1]/div/h3"));
			if(feedbackHeader.getText().toLowerCase().contains("thank you for your payment")) {
				final WebElement orderDetailsLink = browser.findElement(By.xpath("/html/body/div[5]/div[1]/div/div[5]/a[1]"));
				final Pattern regex = Pattern.compile(ORDER_ID_PATTERN);
				final Matcher matcher = regex.matcher(orderDetailsLink.getAttribute("href"));
				if(matcher.find()) {
					final String transactionId = matcher.group(1);
					System.out.println("Successfully parsed transaction id: " + transactionId);
					return new ProcessedOrder(order.id, listing.id, transactionId, "processing");
				} else {
					System.out.println("Could not find order details link...");
				}
			}
		} catch(final NoSuchElementException e) {
			e.printStackTrace();
			System.out.println("submitting the order failed...");
		}
		
		return failedOrder;
	}
	
	private boolean verifyPrice(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		final WebElement priceElement = browser.findElement(By.xpath("//*[@id=\"orders-main\"]/div[3]/table/tfoot/tr/td/div[2]/p[2]/span[2]/b"));
		final String priceStr = priceElement.getText();
		final double parsedPrice = Double.parseDouble(priceStr.replace("US $", "").trim());
		final double maxPrice = order.quantity * listing.listing_max_price;
		System.out.println("parsed price: " + parsedPrice + ", maxPrice: " + maxPrice);
		return parsedPrice <= maxPrice;
	}
	
	private boolean enterPaymentMethod(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		final WebElement useNewCard = browser.findElement(By.className("use-new-card"));
		useNewCard.findElement(By.tagName("input")).click();
		
		//enter details
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[1]/input[1]"))
			.sendKeys(cardInfo.cardNumber);
		
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[2]/div[1]/input[1]"))
			.sendKeys(cardInfo.expMonth);
		
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[2]/div[1]/input[2]"))
			.sendKeys(cardInfo.expYear);
		
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[2]/div[2]/input"))
			.sendKeys(cardInfo.secCode);
		
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[3]/div[1]/input"))
			.sendKeys(cardInfo.firstName);
		
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[3]/div[2]/input"))
			.sendKeys(cardInfo.lastName);
		
		browser.findElement(By.xpath("//*[@id=\"baddress\"]/span")).click();
		
		new Select(browser.findElement(By.xpath("//*[@id=\"baddress-form\"]/select[1]")))
			.selectByVisibleText(cardInfo.country);
		
		browser.findElement(By.xpath("//*[@id=\"baddress-form\"]/input[1]"))
			.sendKeys(cardInfo.streetAddr);
		
		if(!cardInfo.streetAddr2.isEmpty()) {
			browser.findElement(By.xpath("//*[@id=\"baddress-form\"]/input[2]"))
				.sendKeys(cardInfo.streetAddr2);
		}
		
		final WebElement cityInput = browser.findElement(By.xpath("//*[@id=\"baddress-form\"]/input[3]"));
		cityInput.clear();
		cityInput.sendKeys(cardInfo.city);
		
		new Select(browser.findElement(By.xpath("//*[@id=\"baddress-form\"]/select[2]")))
			.selectByVisibleText(cardInfo.state);
		
		final WebElement zipInput = browser.findElement(By.xpath("//*[@id=\"baddress-form\"]/input[4]"));
		zipInput.clear();
		zipInput.sendKeys(cardInfo.zip);
		
		browser.findElement(By.xpath("//*[@id=\"j-payment-method\"]/div[4]/ul/li[1]/div[2]/div/div[8]/button[1]")).click();
			
		return true;
	}
	
	private File takeScreenshot(final WebDriver browser, final WebElement element) {
		try {
			
			((JavascriptExecutor) browser).executeScript("window.scrollTo(document.body.scrollWidth, document.body.scrollHeight)");
			final int bodyWidth = Integer.parseInt(((JavascriptExecutor) browser).executeScript("return document.body.scrollWidth").toString());
			final int bodyHeight = Integer.parseInt(((JavascriptExecutor) browser).executeScript("return document.body.scrollHeight").toString());
			System.out.println("BodyWidth: " + bodyWidth + ", height: " + bodyHeight);
			
			element.click();
			final File screenshot = ((TakesScreenshot)browser).getScreenshotAs(OutputType.FILE);
			final BufferedImage fullImg = ImageIO.read(screenshot);
			System.out.println("fullImg size: " + fullImg.getWidth() + " x " + fullImg.getHeight());
			ImageIO.write(fullImg, "png", new File("full-screen-output.png"));
			
			final Point point = element.getLocation();
			System.out.println("point: " + point);
			final int adjustedImageX = Math.max(0, fullImg.getWidth() - (bodyWidth - point.getX()));
			final int adjustedImageY = Math.max(0, fullImg.getHeight() - (bodyHeight - point.getY()));
			System.out.println("adjustedX: " + adjustedImageX + ", adjustedY: " + adjustedImageY);
			final int width = element.getSize().getWidth();
			final int height = element.getSize().getHeight();
			
			final BufferedImage img = fullImg.getSubimage(adjustedImageX, adjustedImageY, width, height);
			final File imgFile = new File("captcha-image.png");
			ImageIO.write(img, "png", imgFile);
			System.out.println("img size: " + img.getWidth() + " x " + img.getHeight());
			return imgFile;
			
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	private static boolean verifyShippingDetails(final WebDriver browser, final CustomerOrder order, final FulfillmentListing listing) {
		
		System.out.println("verifying buyer name...");
		if(!browser.findElement(By.className("sa-username")).getText().equalsIgnoreCase(order.buyer_name)) {
			return false;
		}
		
		final WebElement locationBlock = browser.findElement(By.className("sa-location"));
		
		System.out.println("populating location items...");
		final List<String> locItems = locationBlock.findElements(By.tagName("li"))
				.stream()
				.map(el -> el.getText())
				.collect(Collectors.toList());
		
		System.out.println("verifying location details...");
		final AtomicInteger matches = new AtomicInteger(0);
		for(final String locItem : locItems) {
			incrementIfMatch(matches, locItem, order.buyer_street_address);
			incrementIfMatch(matches, locItem, order.buyer_apt_suite_unit_etc);
			incrementIfMatch(matches, locItem, order.buyer_city);
			incrementIfMatch(matches, locItem, order.buyer_state_province_region);
			incrementIfMatch(matches, locItem, StateUtils.getStateNameFromCode(order.buyer_state_province_region));
			incrementIfMatch(matches, locItem, order.buyer_zip_postal_code);
			incrementIfMatch(matches, locItem, order.buyer_country);
		}
		
		return matches.intValue() >= 6;	
	}
	
	private static void incrementIfMatch(final AtomicInteger matches, final String toSearch, final String substr) {
		if(toSearch.contains(substr)) {
			matches.incrementAndGet();
		}
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
		
		final List<WebElement> inputs = browser.findElements(By.tagName("input"));
		int inputsComplete = 0;
		for(final WebElement input : inputs) { 
			if(inputsComplete == 4) {
				break;
			}
			System.out.println("checking input: " + input.getAttribute("name"));
			switch(input.getAttribute("name").toLowerCase()) {
				case "contactperson":
					System.out.println("Setting contact name...");
					clearAndTypeInElement(input, order.buyer_name);
					inputsComplete++;
				break;
				case "address":
					System.out.println("Setting street address...");
					clearAndTypeInElement(input, order.buyer_street_address);
					inputsComplete++;
				break;
				case "address2":
					System.out.println("Setting apt / suite / unit / etc...");
					clearAndTypeInElement(input, order.buyer_apt_suite_unit_etc);
					inputsComplete++;
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
					inputsComplete++;
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
