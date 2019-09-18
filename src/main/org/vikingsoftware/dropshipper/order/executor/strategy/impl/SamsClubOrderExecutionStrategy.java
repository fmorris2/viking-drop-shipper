package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
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
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy extends AbstractOrderExecutionStrategy<SamsClubWebDriver> {

	private static final String FAKE_PHONE_NUMBER = "(916) 245-0125";
	private static final int ADDRESS_CHARACTER_LIMIT = 35;
	
	@Override
	protected ProcessedOrder executeOrderImpl(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception {
		return orderItem(order, fulfillmentListing);
	}

	private ProcessedOrder orderItem(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		System.out.println("Getting listing url: " + fulfillmentListing.listing_url);
		driver.get(fulfillmentListing.listing_url);
		
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
		enterAddress(order);
		driver.sleep(2000);
		return finishCheckoutProcess(order, fulfillmentListing);
	}

	private ProcessedOrder finishCheckoutProcess(final CustomerOrder order, final FulfillmentListing listing) throws InterruptedException {

		final double price = verifyConfirmationDetails(order, listing);

		System.out.println("Placing order...");
		try {
			return finalizeOrder(order, listing, price);
		} catch(final Exception e) {
			DBLogging.critical(getClass(), "Failed to submit order: " + order, e);
			System.out.println("submitting the order failed...");
		}

		//THIS IS VERY VERY BAD!!! SAMS CLUB MIGHT HAVE CHANGED THEIR FRONT END? WE SHOULD NO LONGER PROCESS ORDERS
		//AND WE SHOULD NOTIFY DEVELOPERS IMMEDIATELY
		FulfillmentManager.freeze(FulfillmentPlatforms.SAMS_CLUB.getId());
		System.out.println("Submitted an order, but we failed to parse whether it was a success or not. Freezing orders...");
		throw new OrderExecutionException("Failed to parse submitted order: " + order.id);
	}

	private double verifyConfirmationDetails(final CustomerOrder order, final FulfillmentListing listing) {

		//verify shipping details
		System.out.println("Verifying shipping details");
		final WebElement addressEl = driver.findElement(By.className("sc-address"));
		String detailsStr = addressEl.findElement(By.className("sc-address-name")).getText()
				+ " " + addressEl.findElement(By.className("sc-address-street-one")).getText()
				+ " " + addressEl.findElement(By.className("sc-address-city-state")).getText();
		
		try {
			detailsStr += addressEl.findElement(By.className("sc-address-street-two")).getText();
		} catch(final Exception e) {}
		
		detailsStr = detailsStr.toLowerCase();
		
		System.out.println("\tDetails String: " + detailsStr);

		if(!detailsStr.contains(order.buyer_state_province_region.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen state != order state: " + detailsStr + " does not contain " + order.buyer_state_province_region);
		}

		if(!detailsStr.contains(order.buyer_zip_postal_code.substring(0, 4).toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen zip != order zip: " + detailsStr + " does not contain " + order.buyer_zip_postal_code.substring(0, 4));
		}

		System.out.println("Buyer name: " + order.normalizedBuyerName);
		System.out.println("lower case: " + order.normalizedBuyerName.toLowerCase());
		if(!detailsStr.contains(getNormalizedName(order.normalizedBuyerName).toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen details does not contain buyer name: " + detailsStr + " != " + getNormalizedName(order.normalizedBuyerName).toLowerCase());
		}

		final String truncatedAdd = order.buyer_street_address.length() > ADDRESS_CHARACTER_LIMIT 
				? order.buyer_street_address.substring(0, ADDRESS_CHARACTER_LIMIT)
				: order.buyer_street_address;
				
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

		final String total = driver.findElement(By.className("sc-channel-summary-total-price")).getText().replace("$", "");
		System.out.println("\tTotal fulfillment price: " + total);
		final double convertedTotal = Double.parseDouble(total);
		final DecimalFormat df = new DecimalFormat("###.##");
		System.out.println("\tProfit: " + df.format(order.getProfit(convertedTotal)));
		final double profit = order.getProfit(convertedTotal);
		if(profit < 0) {
			throw new OrderExecutionException("WARNING! POTENTIAL FULFILLMENT AT A LOSS FOR FULFILLMENT ID " + listing.id
					+ "! PROFIT: $" + profit);
		}

		System.out.println("\tPricing has been verified.");
		return convertedTotal;
	}

	private ProcessedOrder finalizeOrder(final CustomerOrder order, final FulfillmentListing listing, final double price) throws InterruptedException {

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

		System.out.println("Payment details:");
		System.out.println("\tSubtotal: " + subtotal);
		System.out.println("\tShipping: " + shipping);
		System.out.println("\tSales Tax: " + salesTax);
		System.out.println("\tGrand total: " + price);

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
				throw new OrderExecutionException("Failed to parse order num from Sams Club receipt page!");
			}

			return new ProcessedOrder.Builder()
					.customer_order_id(order.id)
					.fulfillment_listing_id(listing.id)
					.fulfillment_account_id(account.id)
					.fulfillment_transaction_id(orderNum)
					.buy_subtotal(subtotal)
					.buy_sales_tax(salesTax)
					.buy_shipping(shipping)
					.buy_total(price)
					.profit(order.getProfit(price))
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
		driver.findElement(By.cssSelector(".sc-shipping-address-change > span:nth-child(1)"));
		driver.js("document.querySelector(\".sc-shipping-address-change > span:nth-child(1)\").click();");
		System.out.println("\tdone.");
		System.out.println("Clicking edit on preferred address...");
		driver.findElement(By.cssSelector("button.sc-address-card-edit-action:nth-child(1) > span:nth-child(1)"));
		driver.js("document.querySelector(\"button.sc-address-card-edit-action:nth-child(1) > span:nth-child(1)\").click();");
		System.out.println("\tdone.");
		
		driver.sleep(3000);
		//enter details
		System.out.println("Entering name...");
		
		final String normalizedName = getNormalizedName(order.normalizedBuyerName);
		
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"name\"]")), normalizedName);
	
		System.out.println("Entering street address...");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"addressLineOne\"]")), order.buyer_street_address);
		
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"addressLineTwo\"]")), "");
		if(order.buyer_apt_suite_unit_etc != null) {
			System.out.println("Entering apt / suite / bldg...");
			clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"addressLineTwo\"]")), order.buyer_apt_suite_unit_etc);
		}

		System.out.println("Entering city...");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[aria-label=\"City\"]")), order.buyer_city);

		System.out.println("Selecting state " + order.buyer_state_province_region + "...");
		final String fullState = StateUtils.getStateNameFromCode(order.buyer_state_province_region);
		driver.js("document.querySelector('div.sc-select-option[aria-label=\""+fullState+"\"]').click()");

		System.out.println("Entering zip code...");
		final String[] zipCodeParts = order.buyer_zip_postal_code.split("-");
		clearAndSendKeys(driver.findElement(By.cssSelector(".sc-address-form .sc-input-box-container input[name=\"postalCode\"]")), zipCodeParts[0]);
		
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
		
		return normalizedName;
	}

	private void clearAndSendKeys(final WebElement el, final String str) {
		el.sendKeys(Keys.chord(Keys.CONTROL,"a", Keys.DELETE));
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
		driver.get("https://www.samsclub.com/sams/cart/cart.jsp");
	}
	
	private boolean clearErroneousItems(final CustomerOrder order, final FulfillmentListing listing) {
		
		System.out.println("Clearing erroneous items from cart.");
		final Supplier<List<WebElement>> cartItemsSupp = () -> driver.findElements(By.className("nc-v2-cart-item"));
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
				System.out.println("Item: " + item.getAttribute("id"));
				if(item.getAttribute("id") == null) {
					continue;
				}
				final String itemNum = item.findElement(By.cssSelector(".item_no")).getText().split(" ")[2];
				System.out.println("itemNum: " + itemNum + ", listing.item_id: " + listing.item_id);
				if(!itemNum.equals(listing.item_id)) {
					System.out.println("Removing erroneous cart item on page " + driver.getCurrentUrl());
					driver.screenshot("remove_cart_item.png");
					item.findElement(By.className("js_remove")).click();
					driver.sleep(2000); //wait for sluggish sams club to remove the item
					i = 0;
					cartItems = cartItemsSupp.get();
				} else if(Integer.parseInt(item.findElement(By.cssSelector(".nc-item-count")).getAttribute("value")) != order.fulfillment_purchase_quantity) {
					System.out.println("Updating item to correct quantity...");
					clearAndSendKeys(item.findElement(By.cssSelector(".nc-item-count")), Integer.toString(order.fulfillment_purchase_quantity));
				}
			} catch(final StaleElementReferenceException e) {
				System.out.println("Stale element detected - Refreshing cart items collection...");
				i = 0;
				cartItems = cartItemsSupp.get();
			}
		}
		
		driver.sleep(3000);
		verifyCart(order, listing, false);
		return true;
	}

	private void verifyCart(final CustomerOrder order, final FulfillmentListing listing, final boolean correctMistakes) {
		System.out.println("Verifying cart...");
		//verify expected item quantity in cart
		final int numCartItems = Integer.parseInt(driver.findElement(By.id("orderCount")).getText());
		if(numCartItems != order.fulfillment_purchase_quantity) {
			System.out.println("num cart items != fulfillment purchase quantity.");
			if(!correctMistakes || !clearErroneousItems(order, listing)) {
				throw new OrderExecutionException("cart items != order fulfillment quantity: " + numCartItems + " != " + order.fulfillment_purchase_quantity);
			}
		}

		//verify cart items
		final WebElement parentCartTable = driver.findElement(By.className("cart-table"));
		final WebElement cartTable = parentCartTable.findElement(By.tagName("tbody"));
		final List<WebElement> items = cartTable.findElements(By.tagName("tr"));
		if(items.size() > 1) {
			throw new OrderExecutionException("there are more unique items in the cart than expected: " + items.size() + " > 1");
		}

		final WebElement itemRow = items.get(0);
		//verify item details
		final String itemNumStr = itemRow.findElement(By.className("item_no")).getText();
		final String[] itemNumStrParts = itemNumStr.split(" ");
		if(!listing.item_id.equals(itemNumStrParts[itemNumStrParts.length - 1])) {
			throw new OrderExecutionException("Wrong item ID in cart: " + itemNumStrParts[itemNumStrParts.length - 1] + " != " + listing.item_id);
		}

		//verify "ship it" option is picked
		final List<WebElement> deliveryOptions = itemRow.findElements(By.className("nc-delivery"));
		boolean isShipItSelected = false;
		for(final WebElement option : deliveryOptions) {
			try {
				final WebElement input = option.findElement(By.tagName("input"));
				final String value = input.getAttribute("value");
				if(value != null && value.equalsIgnoreCase("online") && input.getAttribute("checked") != null) {
					isShipItSelected = true;
					break;
				}
			} catch(final NoSuchElementException e) {
				driver.findElement(By.cssSelector(".nc-delivery > .nc-ship-only")); // check if "ship only" option is selected
				isShipItSelected = true;
				break;
			}
		}

		if(!isShipItSelected) {
			throw new OrderExecutionException("'Ship it' option is not selected!");
		}

		//verify price!
		final double total = Double.parseDouble(driver.findElement(By.id("nc-v2-est-total")).getText().substring(1));
		if(order.getProfit(total) < 0) { //never automatically sell at a loss....
			throw new OrderExecutionException("WARNING: POTENTIAL FULFILLMENT AT LOSS for fulfillment listing " + listing.id
					+ "! PROFIT: $" + order.getProfit(total));
		}

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

}
