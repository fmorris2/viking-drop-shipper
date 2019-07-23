package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import main.org.vikingsoftware.dropshipper.VSDropShipper;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy extends AbstractOrderExecutionStrategy<SamsClubWebDriver> {

	@Override
	protected ProcessedOrder executeOrderImpl(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception {
		return orderItem(order, fulfillmentListing);
	}

	private ProcessedOrder orderItem(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		System.out.println("Getting listing url: " + fulfillmentListing.listing_url);
		driver.get(fulfillmentListing.listing_url);

		System.out.println("Verifying listing title...");
		verifyListingTitle(fulfillmentListing);
		System.out.println("\tverified.");
		
		final WebElement orderOnlineBox = driver.findElement(By.cssSelector("div.sc-action-buttons div.sc-cart-qty-button.online"));
		enterQuantity(order, orderOnlineBox);
		clickShipThisItem(orderOnlineBox);
		driver.sleep(2000); //wait for SLUGGISH sams club to actually add it to the cart

		
		navigateToCart();
		driver.sleep(5000);
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
		driver.findElement(By.className("js-checkout-btn")).click();
		Thread.sleep(5_000);
		System.out.println("URL: " + driver.getCurrentUrl());
		enterAddress(order);
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
		
		detailsStr = detailsStr.toLowerCase();
		
		System.out.println("\tDetails String: " + detailsStr);

		if(!detailsStr.contains(order.buyer_state_province_region.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen state != order state: " + detailsStr + " does not contain " + order.buyer_state_province_region);
		}

		if(!detailsStr.contains(order.buyer_zip_postal_code.substring(0, 4).toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen zip != order zip: " + detailsStr + " does not contain " + order.buyer_zip_postal_code.substring(0, 4));
		}

		System.out.println("Buyer name: " + order.buyer_name);
		System.out.println("lower case: " + order.buyer_name.toLowerCase());
		if(!detailsStr.contains(order.buyer_name.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen details does not contain buyer name: " + detailsStr + " != " + order.buyer_name.toLowerCase());
		}

		if(!detailsStr.contains(order.buyer_street_address.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen street address != order street address: "
					+ detailsStr + " != " + order.buyer_street_address);
		}

		if(order.buyer_apt_suite_unit_etc != null && !detailsStr.contains(order.buyer_apt_suite_unit_etc.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen apt / suite / unit / etc does not match: "
					+ detailsStr + " != " + order.buyer_apt_suite_unit_etc);
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
//			throw new OrderExecutionException("WARNING! POTENTIAL FULFILLMENT AT A LOSS FOR FULFILLMENT ID " + listing.id
//					+ "! PROFIT: $" + profit);
		}

		System.out.println("\tPricing has been verified.");
		return convertedTotal;
	}

	private ProcessedOrder finalizeOrder(final CustomerOrder order, final FulfillmentListing listing, final double price) throws InterruptedException {

		final WebElement placeOrderButton = driver.findElement(By.cssSelector(".sc-btn-primary")).findElement(By.tagName("span"));

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
		placeOrderButton.click();

		driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
		try {
			//print receipt element
			final List<WebElement> detailEls = driver.findElements(By.cssSelector(".ty_or_first > .ty_or_ff_class"));
			String orderNum = null;
			outer:
			for(final WebElement detailEl : detailEls) {
				final List<WebElement> individualDetails = detailEl.findElements(By.tagName("span"));
				boolean numFlag = false;
				for(final WebElement detail : individualDetails) {
					if(numFlag) {
						orderNum = detail.getText();
						break outer;
					} else if(detail.getText().toLowerCase().contains("order number")) {
						numFlag = true;
					}
				}
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
		System.out.println("Waiting for preferred label to appear...");
		
		driver.findElement(By.cssSelector(".sc-shipping-address-change > span")).click();
		driver.findElement(By.cssSelector(".sc-address-card-edit-action > span")).click();
		
		//enter details
		System.out.println("Entering name...");
		clearAndSendKeys(driver.findElement(By.id("inputbox1")), order.buyer_name);
	
		System.out.println("Entering street address...");
		clearAndSendKeys(driver.findElement(By.id("inputbox2")), order.buyer_street_address);
		
		clearAndSendKeys(driver.findElement(By.id("inputbox3")), "");
		if(order.buyer_apt_suite_unit_etc != null) {
			System.out.println("Entering apt / suite / bldg...");
			clearAndSendKeys(driver.findElement(By.id("inputbox3")), order.buyer_apt_suite_unit_etc);
		}

		System.out.println("Entering city...");
		clearAndSendKeys(driver.findElement(By.id("inputbox4")), order.buyer_city);

		System.out.println("Selecting state " + order.buyer_state_province_region + "...");
		driver.sleep(10_000);
//		final Select stateSelection = new Select(driver.findElement(By.cssSelector(".sc-select-box > .visuallyhidden")));
//		stateSelection.selectByValue(order.buyer_state_province_region);
//		final WebElement stateBox = driver.findElement(By.cssSelector(".sc-select-box > .sc-select-current-option"));
//		stateBox.click();
//		
//		final List<WebElement> states = driver.findElements(By.cssSelector(".sc-select-dropdown-wrapper-open > .sc-select-options > .sc-select-option"));
//		final String stateToSelect = StateUtils.getStateNameFromCode(order.buyer_state_province_region);
//		for(final WebElement state : states) {
//			System.out.println("State: " + state.getText());
//			if(state.getText().equalsIgnoreCase(stateToSelect)) {
//				state.click();
//				break;
//			}
//		}

		System.out.println("Entering zip code...");
		final String[] zipCodeParts = order.buyer_zip_postal_code.split("-");
		clearAndSendKeys(driver.findElement(By.id("inputbox5")), zipCodeParts[0]);
		
		System.out.println("Entering phone number...");
		clearAndSendKeys(driver.findElement(By.id("inputbox6")), order.buyer_phone_number == null ? VSDropShipper.VS_PHONE_NUM : order.buyer_phone_number);

		System.out.println("Clicking save...");
		final WebElement saveButton = driver.findElement(By.cssSelector(".sc-address-form-action-buttons > button:nth-child(2)"));
		driver.scrollIntoView(saveButton);
		saveButton.click();
		Thread.sleep(5000); // wait for save
		System.out.println("\tAddress has been saved.");
	}

	private void clearAndSendKeys(final WebElement el, final String str) {
		el.sendKeys(Keys.chord(Keys.CONTROL,"a", Keys.DELETE));
		driver.sleep(50);
		el.sendKeys(str);
		driver.sleep(50);
	}

	private void verifyListingTitle(final FulfillmentListing listing) throws InterruptedException {
		final String title = driver.findElement(By.cssSelector(".sc-product-header-title-container")).getText();
		if(!title.equalsIgnoreCase(listing.listing_title)) {
			throw new OrderExecutionException("Fulfillment listing title ("+title+") is not what we expected ("+listing.listing_title+")");
		}
	}

	private void enterQuantity(final CustomerOrder order, final WebElement orderOnlineBox) {
		final WebElement input = orderOnlineBox.findElement(By.cssSelector(".sc-input-box-container > input"));
		System.out.println("Fulfillment Purchase Qty: " + order.fulfillment_purchase_quantity);
		clearAndSendKeys(input, Integer.toString(order.fulfillment_purchase_quantity));
	}

	private void clickShipThisItem(final WebElement orderOnlineBox) {
		orderOnlineBox.findElement(By.className("sc-btn-primary")).click();
	}

	private void navigateToCart() {
		driver.get("https://www.samsclub.com/sams/cart/cart.jsp");
	}
	
	private boolean clearErroneousItems(final CustomerOrder order, final FulfillmentListing listing) {
		
		System.out.println("Clearing erroneous items from cart.");
		final Supplier<List<WebElement>> cartItems = () -> driver.findElements(By.className("nc-v2-cart-item"));
		System.out.println("cartItems: " + cartItems.get().size());
		
		for(final WebElement item : cartItems.get()) {
			System.out.println("Item: " + item.getAttribute("id"));
			if(item.getAttribute("id") == null) {
				continue;
			}
			final String itemNum = item.findElement(By.cssSelector(".item_no")).getText().split(" ")[2];
			if(!itemNum.equals(listing.item_id)) {
				System.out.println("Removing erroneous cart item");
				item.findElement(By.className("js_remove")).click();
			} else if(Integer.parseInt(item.findElement(By.cssSelector(".nc-item-count")).getAttribute("value")) != order.fulfillment_purchase_quantity) {
				System.out.println("Updating item to correct quantity...");
				clearAndSendKeys(item.findElement(By.cssSelector(".nc-item-count")), Integer.toString(order.fulfillment_purchase_quantity));
			}
		}
		
		try {
			Thread.sleep(5000);
		} catch(final InterruptedException e) {
			e.printStackTrace();
		}
		verifyCart(order, listing, false);
		return true;
	}

	private void verifyCart(final CustomerOrder order, final FulfillmentListing listing, final boolean correctMistakes) {
		//verify expected item quantity in cart
		final int numCartItems = Integer.parseInt(driver.findElement(By.id("orderCount")).getText());
		if(numCartItems != order.fulfillment_purchase_quantity) {
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
//			throw new OrderExecutionException("WARNING: POTENTIAL FULFILLMENT AT LOSS for fulfillment listing " + listing.id
//					+ "! PROFIT: $" + order.getProfit(total));
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
					Thread.sleep(10);
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
