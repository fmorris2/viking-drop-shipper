package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
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
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.core.utils.CaptchaUtils;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class AliExpressOrderExecutionStrategy implements OrderExecutionStrategy {
	
	private static final String CARD_INFO_PATH = "/data/aliexpress-payment-method.secure";
	private static final String ORDER_ID_PATTERN = "orderId=([^&]*)";
	private static final int ORDER_SUCCESS_WAIT_SECONDS = 180;
	
	private CreditCardInfo cardInfo;
	
	private ProcessedOrder processedOrder;
	private AliExpressWebDriver browser;
	
	public AliExpressOrderExecutionStrategy() {
		parseCardInfo();
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
		browser = new AliExpressWebDriver();
		return browser.getReady();
	}
	
	@Override
	public void finishExecution() {
		browser.close();
	}
	
	@Override
	public ProcessedOrder order(final CustomerOrder customerOrder, final FulfillmentListing fulfillmentListing) {
		processedOrder = new ProcessedOrder.Builder()
				.customer_order_id(customerOrder.id)
				.fulfillment_listing_id(fulfillmentListing.id)
				.build();
			try {
				return executeOrder(customerOrder, fulfillmentListing);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		
		return processedOrder;
	}
	
	private ProcessedOrder executeOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		
		//navigate to fulfillment listing page
		browser.get(fulfillmentListing.listing_url);
		
		System.out.println("setting order quantity...");
		if(!setOrderQuantity(order)) {
			return processedOrder;
		}
		
		//select appropriate order options (color, length, etc)
		System.out.println("selecting order options...");
		final Optional<SkuMapping> skuMapping = SkuMappingManager.getMapping(order.marketplace_listing_id, order.sku);
		if(!skuMapping.isPresent()) {
			return processedOrder;
		}
		
		if(!browser.selectOrderOptions(skuMapping.get(), fulfillmentListing)) {
			return processedOrder;
		}
		
		browser.findElement(By.id("j-buy-now-btn")).click();
		
		System.out.println("entering shipping address...");
		if(!enterShippingAddress(order, fulfillmentListing)) {
			return processedOrder;
		}
		
		System.out.println("saving shipping address...");
		browser.findElement(By.className("sa-confirm")).click();
		
		System.out.println("refreshing page...");
		browser.navigate().refresh();
		
		System.out.println("verifying shipping details...");
		if(!verifyShippingDetails(order, fulfillmentListing)) {
			return processedOrder;
		}
		
		System.out.println("selecting payment method...");
		if(!enterPaymentMethod(order, fulfillmentListing)) {
			return processedOrder;
		}
		
		System.out.println("verifying final order price...");
		final double finalOrderPrice = parseFinalOrderPrice();
		if(finalOrderPrice > fulfillmentListing.listing_max_price * order.quantity) {
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

		return OrderExecutor.isTestMode ? new ProcessedOrder.Builder()
				.customer_order_id(order.id)
				.fulfillment_listing_id(fulfillmentListing.id)
				.fulfillment_transaction_id("test_trans_id")
				.sale_price(finalOrderPrice)
				.quantity(order.quantity)
				.order_status("test")
				.build()
			   : finalizeOrder(order, fulfillmentListing, finalOrderPrice);
	}
	
	private double parseFinalOrderPrice() {
		try {
			final String unparsedPrice = browser.findElement(By.id("total-price-fee-amount-post-currency")).getAttribute("value");
			return Double.parseDouble(unparsedPrice);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return Double.MAX_VALUE;
	}
	
	private ProcessedOrder finalizeOrder(final CustomerOrder order, final FulfillmentListing listing, final double finalOrderPrice) {
		System.out.println("clicking confirm & pay...");
		browser.findElement(By.id("place-order-btn")).click();
		
		browser.manage().timeouts().implicitlyWait(ORDER_SUCCESS_WAIT_SECONDS, TimeUnit.SECONDS);
		
		final ProcessedOrder.Builder builder = new ProcessedOrder.Builder()
			.customer_order_id(order.id)
			.fulfillment_listing_id(listing.id)
			.sale_price(finalOrderPrice)
			.quantity(order.quantity)
			.order_status("attempted to order (status unknown)");
				
		try {
			final WebElement feedbackHeader = browser.findElement(By.xpath("/html/body/div[5]/div[1]/div/h3"));
			if(feedbackHeader.getText().toLowerCase().contains("thank you for your payment")) {
				final WebElement orderDetailsLink = browser.findElement(By.xpath("/html/body/div[5]/div[1]/div/div[5]/a[1]"));
				final Pattern regex = Pattern.compile(ORDER_ID_PATTERN);
				final Matcher matcher = regex.matcher(orderDetailsLink.getAttribute("href"));
				if(matcher.find()) {
					final String transactionId = matcher.group(1);
					System.out.println("Successfully parsed transaction id: " + transactionId);
					return builder
						.fulfillment_transaction_id(transactionId)
						.order_status("successfully ordered")
						.build();
				} else {
					System.out.println("Could not find order details link...");
				}
			}
		} catch(final NoSuchElementException e) {
			e.printStackTrace();
			System.out.println("submitting the order failed...");
		}
		
		//THIS IS VERY VERY BAD!!! ALIEXPRESS MIGHT HAVE CHANGED THEIR FRONT END? WE SHOULD NO LONGER PROCESS ORDERS
		//AND WE SHOULD NOTIFY DEVELOPERS IMMEDIATELY
		OrderExecutor.freezeOrders = true;
		System.out.println("Submitted an order, but we failed to parse whether it was a success or not. Freezing orders...");
		
		return builder
				.fulfillment_transaction_id(null)
				.build();
	}
	
	private boolean enterPaymentMethod(final CustomerOrder order, final FulfillmentListing listing) {
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
	
	private boolean verifyShippingDetails(final CustomerOrder order, final FulfillmentListing listing) {
		
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
	
	private boolean enterShippingAddress(final CustomerOrder order, final FulfillmentListing listing) {
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
	
	private void clearAndTypeInElement(final WebElement element, final String str) {
		element.clear();
		
		if(str != null) {
			element.sendKeys(str);
		}
	}
	
	private boolean setOrderQuantity(final CustomerOrder order) {
		//quantity input
		if(order.quantity > 1) {
			final WebElement quantityInput = browser.findElement(By.id("j-p-quantity-input"));
			if(quantityInput == null) {
				return false;
			}
			
			quantityInput.sendKeys(Keys.BACK_SPACE);
			quantityInput.sendKeys(Integer.toString(order.quantity));
		}
		
		return true;
	}
}
