package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import com.google.api.client.util.Key;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.misc.StateUtils;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.SamsClubUtils;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsProductAPI;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.executor.error.FatalOrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy extends AbstractOrderExecutionStrategy<SamsClubWebDriver> {

	private static final String FAKE_PHONE_NUMBER = "(916) 245-0125";
	private static final int ADDRESS_CHARACTER_LIMIT = 35;
	private static final SamsProductAPI productApi = new SamsProductAPI();
	
	/*
	 * TODO - Use column in DB so we can toggle it on front-end
	 */
	private boolean shouldDisregardProfit(final CustomerOrder order) {
		return false;
	}
	
	@Override
	protected ProcessedOrder executeOrderImpl(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception {
		try {
			return orderItem(order, fulfillmentListing);
		} catch(final Exception e) {
			throw new RuntimeException(e);
		} finally {
			driver.clearSession();
			driver.manage().deleteAllCookies();
		}
	}

	private ProcessedOrder orderItem(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		System.out.println("Getting listing url: " + fulfillmentListing.listing_url);
		//rakuten test
		//driver.get("https://www.samsclub.com/?pid=_Aff_LS&siteID=AysPbYF8vuM-_xxRlVgh.wbES7nekckO5w&ranMID=38733&ranEAID=AysPbYF8vuM&ranSiteID=AysPbYF8vuM-_xxRlVgh.wbES7nekckO5w&pubNAME=Ebates.com");
		
		driver.get(fulfillmentListing.listing_url);
		
		productApi.parse(fulfillmentListing.product_id);
		
		if(productApi.getAvailableToSellQuantity().orElse(0) <= 0) {
			System.out.println("Listing is out of stock!");
			throw new OrderExecutionException("Listing " + fulfillmentListing.listing_url + " is out of stock!");
		}
		
		System.out.println("Finding orderOnlineBox...");
		final WebElement orderOnlineBox = driver.findElement(By.cssSelector("div.sc-action-buttons div.sc-cart-qty-button.online"));
		System.out.println("\tfound.");
		clickShipThisItem(orderOnlineBox);
		driver.sleep(2000); //wait for SLUGGISH sams club to actually add it to the cart
	
		navigateToCart();
		try {
			verifyCart(order, fulfillmentListing, true);
		} catch(final OrderExecutionException e) {
			e.printStackTrace();
			driver.manage().timeouts().implicitlyWait(250, TimeUnit.MILLISECONDS);
			clearCart();
			driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			throw e;
		}

		//click checkout
		System.out.println("Initial details verified. Beginning checkout process... from URL " + driver.getCurrentUrl());
		driver.screenshot("cart-pre-checkout.png");
		driver.findElement(By.cssSelector(".summary .continue-btn .js-checkout-btn"));
		driver.js("document.querySelector(\".summary .continue-btn .js-checkout-btn\").click();");
		driver.screenshot("enter-address-page.png");
		driver.sleep(5000);
		enterAddress(order);
		driver.sleep(5000);
		return finishCheckoutProcess(order, fulfillmentListing);
	}

	private ProcessedOrder finishCheckoutProcess(final CustomerOrder order, final FulfillmentListing listing) throws InterruptedException {

		final SamsPricingDetails pricingDetails = verifyConfirmationDetails(order, listing);

		System.out.println("Placing order...");
		try {
			return finalizeOrder(order, listing, pricingDetails);
		} catch(final FatalOrderExecutionException e) {
			DBLogging.critical(getClass(), "Failed to submit customer order with id: " + order.id, e);
			System.out.println("submitting the order failed...");
			
			//THIS IS VERY VERY BAD!!! SAMS CLUB MIGHT HAVE CHANGED THEIR FRONT END? WE SHOULD NO LONGER PROCESS ORDERS
			//AND WE SHOULD NOTIFY DEVELOPERS IMMEDIATELY
			FulfillmentManager.disableOrderExecution(FulfillmentPlatforms.SAMS_CLUB.getId());
			System.out.println("Submitted an order, but we failed to parse whether it was a success or not. Freezing orders...");
			throw new OrderExecutionException("Failed to parse submitted order: " + order.id + ". Ended up on URL: "
					+ driver.getCurrentUrl());
		}
	}

	private SamsPricingDetails verifyConfirmationDetails(final CustomerOrder order, final FulfillmentListing listing) {

		//verify shipping details
		System.out.println("Verifying shipping details");
		final WebElement addressEl = driver.findElement(By.className("sc-address"));
		String detailsStr = addressEl.findElement(By.className("sc-address-name")).getText()
				+ " " + addressEl.findElement(By.className("sc-address-street-one")).getText()
				+ " " + addressEl.findElement(By.className("sc-address-city-state")).getText();
		
		try {
			driver.setImplicitWait(1);
			detailsStr += driver.findElementNormal(By.cssSelector(".sc-address .sc-address-street-two")).getText();
		} catch(final Exception e) {}
		driver.resetImplicitWait();
		
		detailsStr = detailsStr.toLowerCase();
		
		System.out.println("\tDetails String: " + detailsStr);

		final String stateCode = StateUtils.isStateCode(order.buyer_state_province_region) 
				? order.buyer_state_province_region
				: StateUtils.getCodeFromStateName(order.buyer_state_province_region);
		
		if(!detailsStr.contains(stateCode.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen state != order state: " + detailsStr + " does not contain " + stateCode);
		}

		if(!detailsStr.contains(order.buyer_zip_postal_code.substring(0, 4).toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen zip != order zip: " + detailsStr + " does not contain " + order.buyer_zip_postal_code.substring(0, 4));
		}

		System.out.println("Buyer name: " + order.normalizedBuyerName);
		System.out.println("lower case: " + order.normalizedBuyerName.toLowerCase());
		if(!detailsStr.contains(getNormalizedName(order.normalizedBuyerName).toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen details does not contain buyer name: " + detailsStr + " != " + getNormalizedName(order.normalizedBuyerName).toLowerCase());
		}

		String truncatedAdd = order.buyer_street_address.length() > ADDRESS_CHARACTER_LIMIT 
				? order.buyer_street_address.substring(0, ADDRESS_CHARACTER_LIMIT)
				: order.buyer_street_address;
		truncatedAdd = truncatedAdd.replaceAll("\\s{2,}", " ");
				
		if(!detailsStr.contains(truncatedAdd.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen street address != order street address: "
					+ detailsStr + " != " + truncatedAdd);
		}

		final String truncatedAdd2 = order.buyer_apt_suite_unit_etc != null 
				&& order.buyer_apt_suite_unit_etc.length() > ADDRESS_CHARACTER_LIMIT
				? order.buyer_apt_suite_unit_etc.substring(0, ADDRESS_CHARACTER_LIMIT)
				: order.buyer_apt_suite_unit_etc;
				
		if(truncatedAdd2 != null && !detailsStr.contains(truncatedAdd2.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen apt / suite / unit / etc does not match: "
					+ detailsStr + " != " + truncatedAdd2);
		}

		//verify pricing again...
		System.out.println("\tShipping details have been verified.");
		System.out.println("Verifying pricing...");

		final String total = driver.findElement(By.cssSelector(".sc-channel-summary-total-total .sc-channel-summary-total-price")).getText().replace("$", "");
		System.out.println("\tTotal fulfillment price: " + total);
		final double convertedTotal = Double.parseDouble(total);
		
		double productFees = 0D;
		try {
			driver.setImplicitWait(1);
			final String productFeesStr = driver.findElementNormal(By.cssSelector(".sc-channel-summary-total-product-fees .sc-channel-summary-total-price")).getText().replace("$", "");
			productFees = Double.parseDouble(productFeesStr);
		} catch(final Exception e) {
			//swallow
		} finally {
			driver.resetImplicitWait();
		}
		
		System.out.println("\tProduct Fees: " + productFees);
		
		final SamsPricingDetails pricingDetails = new SamsPricingDetails(productFees, convertedTotal);

		System.out.println("\tPricing has been verified.");
		return pricingDetails;
	}

	private ProcessedOrder finalizeOrder(final CustomerOrder order, final FulfillmentListing listing, 
			final SamsPricingDetails pricingDetails) throws InterruptedException {

		final String placeOrderButtonSelector = ".sc-checkout-main .sc-btn-primary > span";
		driver.findElement(By.cssSelector(placeOrderButtonSelector));

		final WebElement orderSummary = driver.findElement(By.cssSelector(".sc-order-summary > .sc-channel-summary"));
		final List<WebElement> tableRows = orderSummary.findElement(By.tagName("table")).findElements(By.tagName("tr"));

		double subtotal = 0, shipping = 0, salesTax = 0;
		
		for(final WebElement row : tableRows) {
			final String rowTitle = row.findElement(By.tagName("td")).getText().toLowerCase();
			final String rowContent = row.findElement(By.cssSelector("td > span")).getText().toLowerCase();
			if(rowTitle.contains("subtotal")) {
				subtotal = Double.parseDouble(rowContent.replace("$", ""));
			} else if(rowTitle.contains("shipping")) {
				shipping = rowContent.contains("free") ? 0.0D : Double.parseDouble(rowContent.replace("$", ""));
			} else if(rowTitle.contains("sales tax")) {
				salesTax = Double.parseDouble(rowContent.replace("$", ""));
			}
		}
		
		if(shipping > 0) {
			flagAccountLostFreeShipping();
		}
		
		final DecimalFormat df = new DecimalFormat("###.##");
		System.out.println("\tProfit: " + df.format(order.getProfit(pricingDetails.total)));
		final double profit = order.getProfit(pricingDetails.total);
		if(!shouldDisregardProfit(order) && profit < 0) {
			throw new OrderExecutionException("WARNING! POTENTIAL FULFILLMENT AT LOSS FOR FULFILLMENT ID " + listing.id
					+ "! PROFIT: $" + profit);
		}

		System.out.println("Payment details:");
		System.out.println("\tSubtotal: " + subtotal);
		System.out.println("\tShipping: " + shipping);
		System.out.println("\tSales Tax: " + salesTax);
		System.out.println("\tProduct Fees: " + pricingDetails.productFees);
		System.out.println("\tGrand total: " + pricingDetails.total);

		System.out.println("Clicking place order button...");
		if(OrderExecutor.isTestMode) {
			System.out.println("TEST MODE - SUCCESS!");
			return processedOrder;
		}
		
		driver.js("document.querySelector(\""+placeOrderButtonSelector+"\").click();");
		
		Thread.sleep(2500); //inital sleep after order button

		try {
			String orderNum = null;
			final long start = System.currentTimeMillis();
			while(orderNum == null && System.currentTimeMillis() - start < 60_000) {
				System.out.println("Attempting to parse order number from page source.");
				orderNum = SamsClubUtils.getOrderNumberFromPageSource(driver.getPageSource());
				driver.savePageSource("last-attempted-sams-club-order.html");
				Thread.sleep(500);
			}
			
			System.out.println("orderNum: " + orderNum);
			
			if(orderNum == null) {
				throw new FatalOrderExecutionException("Failed to parse order num from Sams Club receipt page!");
			}

			return new ProcessedOrder.Builder()
					.customer_order_id(order.id)
					.fulfillment_listing_id(listing.id)
					.fulfillment_account_id(account.id)
					.fulfillment_transaction_id(orderNum)
					.buy_subtotal(subtotal)
					.buy_sales_tax(salesTax)
					.buy_shipping(shipping)
					.buy_product_fees(pricingDetails.productFees)
					.buy_total(pricingDetails.total)
					.profit(order.getProfit(pricingDetails.total))
					.date_processed(System.currentTimeMillis())
					.build();
		} finally {
			driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
		}
	}

	private void enterAddress(final CustomerOrder order) throws InterruptedException {
		System.out.println("Clicking change address...");
		driver.savePageSource("pre-enter-address.html");

		driver.setImplicitWait(5);
		System.out.println("current url: " + driver.getCurrentUrl());
		try {
		driver.findElementNormal(By.cssSelector(".sc-shipping-address-change > span:nth-child(1)"));
		} catch(final NoSuchElementException e) {
			try {
				driver.js("document.querySelector(\".sc-btn-secondary > span:nth-child(1)\").click();");
				enterAddress(order);
				return;
			} catch(final Exception ex) {
				//swallow
			}
		}
		try {
			driver.js("document.querySelector(\".sc-btn-secondary > span:nth-child(1)\").click();");
		} catch(final JavascriptException e) {
			//swallow
		}
		
		try {
			driver.js("document.querySelector(\".sc-shipping-address-change > span:nth-child(1)\").click();");
		} catch(final JavascriptException e) {
			//swallow
		}
		System.out.println("\tdone.");
		System.out.println("Clicking edit on preferred address...");
		driver.findElementNormal(By.cssSelector("button.sc-address-card-edit-action:nth-child(1) > span:nth-child(1)"));
		driver.js("document.querySelector(\"button.sc-address-card-edit-action:nth-child(1) > span:nth-child(1)\").click();");
		System.out.println("\tdone.");
		
		//driver.sleep(3000);
		//enter details
		System.out.println("Entering name...");
		
		final String normalizedName = getNormalizedName(order.normalizedBuyerName);
		
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"name\"]")), normalizedName);
	
		System.out.println("Entering street address...");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"addressLineOne\"]")), order.buyer_street_address);
		
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"addressLineTwo\"]")), "", true);
		if(order.buyer_apt_suite_unit_etc != null) {
			System.out.println("Entering apt / suite / bldg...");
			clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"addressLineTwo\"]")), order.buyer_apt_suite_unit_etc);
		}

		System.out.println("Entering city...");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[aria-label=\"City\"]")), order.buyer_city);

		System.out.println("Selecting state " + order.buyer_state_province_region + "...");
		String stateSelector;
		if(StateUtils.isStateCode(order.buyer_state_province_region)) {
			stateSelector = StateUtils.getStateNameFromCode(order.buyer_state_province_region);
		} else if(StateUtils.isFullStateName(order.buyer_state_province_region)) {
			stateSelector = order.buyer_state_province_region;
		} else {
			throw new OrderExecutionException("Failed to identify state selector for customer provided state: " + order.buyer_state_province_region);
		}
		driver.js("document.querySelector('div.sc-select-option[aria-label=\""+stateSelector+"\"]').click()");

		final String[] zipCodeParts = order.buyer_zip_postal_code.split("-");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"postalCode\"]")), zipCodeParts[0], true);
		
		System.out.println("Entering phone number...");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"phone\"]")), FAKE_PHONE_NUMBER);

		System.out.println("Clicking save...");
		driver.findElement(By.cssSelector(".sc-address-form-action-buttons > button:nth-child(2)"));
		driver.js("document.querySelector(\".sc-address-form-action-buttons > button:nth-child(2)\").click();");
		System.out.println("\tAddress has been saved.");
	}
	
	private String getNormalizedName(final String name) {
		String normalizedName = "";
		final String[] nameParts = name.split(" ");
		for(int i = 0; i < 4 && i < nameParts.length; i++) {
			if(i > 0) {
				normalizedName += " ";
			}
			normalizedName += nameParts[i];
		}
		
		if(nameParts.length == 1) { //sams club requires two words with a space between them.
			normalizedName += " " + normalizedName;
		}
		
		normalizedName = normalizedName.replace("`", "").replace("'", "");
		
		return normalizedName;
	}
	
	private void clearAndSendKeys(final WebElement el, final String str) {
		clearAndSendKeys(el, str, false);
	}

	private void clearAndSendKeys(final WebElement el, final String str, final boolean cantCtrlA) {
		if(cantCtrlA) {
			for(int i = 0; i < 30; i++) {
				el.sendKeys(Keys.BACK_SPACE);
			}
		} else {
			el.sendKeys(Keys.chord(Keys.CONTROL,"a", Keys.DELETE));
		}
		driver.sleep(50);
		el.sendKeys(str);
		driver.sleep(50);
	}

	private void clickShipThisItem(final WebElement orderOnlineBox) {
		System.out.println("Clicking ship this item...");
		driver.js("document.querySelector(\"div.sc-action-buttons div.sc-cart-qty-button.online .sc-btn-primary span\").click();");
		driver.sleep(500);
		System.out.println("\tdone.");
	}

	private void navigateToCart() {
		final String url = "https://www.samsclub.com/sams/cart/cart.jsp";
		driver.get(url);
		
		try {
			//"Sorry there's a problem" message, for example
			driver.setImplicitWait(2);
			final WebElement errorEl = driver.findElementNormal(By.className("js-global-error"));
			if(errorEl != null && errorEl.isDisplayed()) {
				System.out.println("Encountered error message on cart. Retrying...");
				navigateToCart();
			}
		} catch(final NoSuchElementException e) {
			//swallow
		}
	}
	
	private boolean clearErroneousItems(final CustomerOrder order, final FulfillmentListing listing) {
		
		System.out.println("Clearing erroneous items from cart.");
		final Supplier<List<WebElement>> cartItemsSupp = () -> driver.findElements(By.cssSelector(".cart-table .nc-v2-cart-item"));
		System.out.println("cartItems: " + cartItemsSupp.get().size());
		
		List<WebElement> cartItems = cartItemsSupp.get();
		int attempts = 0;
		for(int i = 0; i < cartItems.size(); i++) {
			if(attempts >= 30) {
				break;
			}
			attempts++;
			try {
				final WebElement item = cartItems.get(i);
				final String id = item.getAttribute("id");
				System.out.println("Item: " + id);
				if(id == null || id.isEmpty()) {
					System.out.println("id == null || id is empty - Skipping...");
					continue;
				}
				driver.setImplicitWait(1);
				final String itemNum = item.findElement(By.cssSelector(".item_no")).getText().split(" ")[2];
				System.out.println("itemNum: " + itemNum + ", listing.item_id: " + listing.item_id);
				if(!itemNum.equals(listing.item_id)) {
					System.out.println("Removing erroneous cart item on page " + driver.getCurrentUrl());
					driver.screenshot("remove_cart_item.png");
					item.findElement(By.className("js_remove")).click();
					driver.sleep(3000); //wait for sluggish sams club to remove the item
					i = 0;
					cartItems = cartItemsSupp.get();
				} else if(Integer.parseInt(item.findElement(By.cssSelector(".nc-item-count")).getAttribute("value")) != order.fulfillment_purchase_quantity) {
					System.out.println("Updating item to correct quantity...");
					clearAndSendKeys(item.findElement(By.cssSelector(".nc-item-count")), Integer.toString(order.fulfillment_purchase_quantity), true);
					driver.sleep(3000); //wait for sluggish sams club to remove the item
				}
			} catch(final StaleElementReferenceException e) {
				System.out.println("Stale element detected - Refreshing cart items collection...");
				i = 0;
				cartItems = cartItemsSupp.get();
			} catch(final NoSuchElementException e) {
				System.out.println("Could not find element");
				e.printStackTrace();
				driver.savePageSource("sams-cart.html");
			}
		}
		
		verifyCart(order, listing, false);
		return true;
	}

	private void verifyCart(final CustomerOrder order, final FulfillmentListing listing, final boolean correctMistakes) {
		System.out.println("Verifying cart...");
		//verify expected item quantity in cart
		final WebElement numCartItemsEl = driver.findElementNormal(By.id("orderCount"));
		if(numCartItemsEl == null) {
			throw new OrderExecutionException("Could not find number of cart items element w/ id 'orderCount'");
		}
		
		final int numCartItems = Integer.parseInt(numCartItemsEl.getText());
		System.out.println("numCartItems: " + numCartItems);
		if(numCartItems != order.fulfillment_purchase_quantity) {
			System.out.println("num cart items != fulfillment purchase quantity.");
			if(!correctMistakes || !clearErroneousItems(order, listing)) {
				throw new OrderExecutionException("cart items != order fulfillment quantity: " + numCartItems + " != " + order.fulfillment_purchase_quantity);
			}
		}

		//verify cart items
		System.out.println("Verifying number of cart items...");
		final WebElement parentCartTable = driver.findElementNormal(By.className("cart-table"));
		final WebElement cartTable = parentCartTable.findElement(By.tagName("tbody"));
		final List<WebElement> items = cartTable.findElements(By.tagName("tr"));
		if(items.size() > 1) {
			throw new OrderExecutionException("there are more unique items in the cart than expected: " + items.size() + " > 1");
		}
		
		System.out.println("\tdone.");

		System.out.println("Verifying item number...");
		final WebElement itemRow = items.get(0);
		//verify item details
		final String itemNumStr = itemRow.findElement(By.className("item_no")).getText();
		final String[] itemNumStrParts = itemNumStr.split(" ");
		if(!listing.item_id.equals(itemNumStrParts[itemNumStrParts.length - 1])) {
			throw new OrderExecutionException("Wrong item ID in cart: " + itemNumStrParts[itemNumStrParts.length - 1] + " != " + listing.item_id);
		}
		System.out.println("\tdone.");
		
		System.out.println("Verifying we have free shipping...");
		if(!hasFreeShippingInCart()) {
			flagAccountLostFreeShipping();
		}

		System.out.println("Verifying price...");
		//verify price!
		final double total = Double.parseDouble(driver.findElementNormal(By.id("nc-v2-est-total")).getText().substring(1));
		if(!shouldDisregardProfit(order) && order.getProfit(total) < 0) { //never automatically sell at a loss....
			throw new OrderExecutionException("WARNING: POTENTIAL FULFILLMENT AT LOSS for fulfillment listing " + listing.id
					+ "! PROFIT: $" + order.getProfit(total));
		}
		System.out.println("\tdone.");
	}
	
	private void flagAccountLostFreeShipping() {
		FulfillmentAccountManager.get().markAccountAsDisabled(account);
		throw new OrderExecutionException("Sam's Club Account has lost free shipping: " + account);
	}
	
	private boolean hasFreeShippingInCart() {
		final String shippingText = driver.findElement(By.id("nc-v2-est-shipping")).getText();
		System.out.println("Shipping text: " + shippingText);
		if(shippingText.equalsIgnoreCase("free") || shippingText.equalsIgnoreCase("--")) {
			return true;
		}
		
		final double shipping = Double.parseDouble(shippingText.contains("$") ? shippingText.substring(1) : shippingText);
		return shipping <= 0;
	}

	private void clearCart() {
		try {
			final Supplier<WebElement> parentCartTable = () -> driver.findElement(By.className("cart-table"));
			final Supplier<WebElement> cartTable = () -> parentCartTable.get().findElement(By.tagName("tbody"));
			final Supplier<List<WebElement>> removeEls = () -> cartTable.get().findElements(By.className("js_remove"));

			System.out.println("clearing cart...");
			final Supplier<Integer> numCartItemsSupp = () -> Integer.parseInt(driver.findElement(By.cssSelector("#orderCount")).getText());
			int currentNumCartItems = numCartItemsSupp.get();
			List<WebElement> currentEls;
			startLoop();
			System.out.println("Cart items to remove: " + removeEls.get().size());
			while(!(currentEls = removeEls.get()).isEmpty() && !hasExceededThreshold()) {
				final WebElement el = currentEls.get(0);
				System.out.println("removing cart item: " + el);
				el.click();
				final long start = System.currentTimeMillis();
				while(currentNumCartItems == numCartItemsSupp.get() && (System.currentTimeMillis() - start < LOOP_THRESHOLD)) {
					driver.sleep(10);
				}

				currentNumCartItems = numCartItemsSupp.get();
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Class<? extends DriverSupplier<SamsClubWebDriver>> getDriverSupplierClass() {
		return SamsClubDriverSupplier.class;
	}
	
	private static class SamsPricingDetails {
		public final double productFees;
		public final double total;
		
		public SamsPricingDetails(final double productFees, final double total) {
			this.productFees = productFees;
			this.total = total;
		}
	}

}
