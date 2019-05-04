package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.VSDropShipper;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.misc.StateUtils;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy extends AbstractOrderExecutionStrategy<SamsClubWebDriver> {

	private String lastWebPageTitle = "";

	@Override
	protected ProcessedOrder executeOrderImpl(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception {
		enterAddress(order);
		return orderItem(order, fulfillmentListing);
	}

	private ProcessedOrder orderItem(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		driver.get(fulfillmentListing.listing_url);

		verifyListingTitle(fulfillmentListing);

		final WebElement orderOnlineBox = driver.findElement(By.cssSelector("div.sc-action-buttons div.sc-cart-qty-button.online"));
		enterQuantity(order, orderOnlineBox);
		clickShipThisItem(orderOnlineBox);
		Thread.sleep(2000); //wait for SLUGGISH sams club to actually add it to the cart
		navigateToCart();
		try {
			verifyCart(order, fulfillmentListing);
		} catch(final OrderExecutionException e) {
			e.printStackTrace();
			driver.manage().timeouts().implicitlyWait(250, TimeUnit.MILLISECONDS);
			clearCart();
			driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			throw e;
		}

		//click checkout
		System.out.println("Initial details verified. Beginning checkout process...");
		driver.findElement(By.className("js-checkout-btn")).click();
		driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
		try {
			driver.findElement(By.cssSelector(".placeorderbottom > .cxo-a-green"));
		} finally {
			driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
		}

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
		final WebElement addressEl = driver.findElement(By.className("address"));
		final String stateAbbrev = addressEl.getAttribute("data-state");
		final String zipCode = addressEl.getAttribute("data-postalcode");
		final List<WebElement> otherDetailEls = addressEl.findElements(By.tagName("dd"));
		String otherDetailsStr = "";
		for(final WebElement el : otherDetailEls) {
			otherDetailsStr += el.getText().replaceAll("  ", " ");
		}

		otherDetailsStr = otherDetailsStr.toLowerCase();

		System.out.println("\tState Abbrev: " + stateAbbrev);
		System.out.println("\tZip: " + zipCode);
		System.out.println("\tOther Details String: " + otherDetailsStr);

		if(!stateAbbrev.equalsIgnoreCase(order.buyer_state_province_region)) {
			throw new OrderExecutionException("Confirmation screen state != order state: " + stateAbbrev + " != " + order.buyer_state_province_region);
		}

		if(!order.buyer_zip_postal_code.contains(zipCode)) {
			throw new OrderExecutionException("Confirmation screen zip != order zip: " + zipCode + " != " + order.buyer_zip_postal_code);
		}

		if(!otherDetailsStr.contains(order.buyer_name.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen details does not contain buyer name: " + otherDetailsStr + " != " + order.buyer_name);
		}

		if(!otherDetailsStr.contains(order.buyer_street_address.toLowerCase())) {
			throw new OrderExecutionException("Confirmation screen street address != order street address: "
					+ otherDetailsStr + " != " + order.buyer_street_address);
		}

		if(order.buyer_apt_suite_unit_etc != null && !otherDetailsStr.contains(order.buyer_apt_suite_unit_etc)) {
			throw new OrderExecutionException("Confirmation screen apt / suite / unit / etc does not match: "
					+ otherDetailsStr + " != " + order.buyer_apt_suite_unit_etc);
		}

		//verify pricing again...
		System.out.println("\tShipping details have been verified.");
		System.out.println("Verifying pricing...");

		final String total = driver.findElement(By.cssSelector(".grandTotal > .pull-right")).getText().replace("$", "");
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

		final WebElement placeOrderButton = driver.findElement(By.cssSelector(".placeorderbottom > .cxo-a-green"));

		final WebElement paymentDetailsEl = driver.findElements(By.className("thirdCol"))
				.stream().filter(el -> el.getAttribute("class").contains("orderConfirmation")).findFirst().orElse(null);

		if(paymentDetailsEl == null) {
			throw new OrderExecutionException("Could not parse payment details on confirmation screen...");
		}

		double subtotal = 0, shipping = 0, salesTax = 0, fees = 0;

		final List<WebElement> paymentDetails = paymentDetailsEl.findElements(By.tagName("dd"));

		for(final WebElement detailGroup : paymentDetails) {
			final List<WebElement> individualDetails = detailGroup.findElements(By.tagName("span"));
			for(final WebElement detail : individualDetails) {
				final String txt = detail.getText().trim().toLowerCase();
				System.out.println("txt: " + txt);
				if(txt == null || txt.isEmpty()) {
					continue;
				} else if(subtotal == -1) {
					subtotal = Double.parseDouble(txt.substring(1));
				} else if(shipping == -1) {
					shipping = txt.contains("free") ? 0 : Double.parseDouble(txt.substring(1));
				} else if(salesTax == -1) {
					salesTax = Double.parseDouble(txt.substring(1));
				} else if(fees == -1) {
					fees = Double.parseDouble(txt.substring(1));
				} else if(txt.contains("subtotal")) {
					subtotal = -1;
				} else if(txt.contains("shipping")) {
					shipping = -1;
				} else if(txt.contains("sales tax")) {
					salesTax = -1;
				} else if(txt.contains("product fee")) {
					fees = -1;
				}
			}
		}

		System.out.println("Payment details:");
		System.out.println("\tSubtotal: " + subtotal);
		System.out.println("\tShipping: " + shipping);
		System.out.println("\tSales Tax: " + salesTax);
		System.out.println("\tProduct Fees: " + fees);
		System.out.println("\tGrand total: " + price);

		System.out.println("Clicking place order button...");
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
					.buy_product_fees(fees)
					.buy_total(price)
					.profit(order.getProfit(price))
					.build();
		} finally {
			driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
		}
	}

	private void enterAddress(final CustomerOrder order) throws InterruptedException {
		driver.get("https://www.samsclub.com/account/address-setting?xid=account_address-book");
		System.out.println("Waiting for preferred label to appear...");
		driver.findElement(By.className("sc-membership-address-setting-preferred-label")); //wait for JS to load
		System.out.println("\tpreferred label has been loaded.");
		final String cssSelector = ".sc-membership-address-setting > div > div:first-child .sc-tile-tile-actions > button.sc-tile-edit-action";
		final WebElement editButton = driver.findElement(By.cssSelector(cssSelector));
		scrollToTopOfPage();
		editButton.findElement(By.tagName("span")).click();

		//enter details
		final String[] nameParts = order.buyer_name.split(" ");
		String firstName = "";
		for(int i = 0; i < nameParts.length - 1; i++) {
			firstName += (i > 0 ? " " : "") + nameParts[i];
		}
		System.out.println("Entering first name...");
		clearAndSendKeys(driver.findElement(By.id("inputbox3")), firstName);
		System.out.println("Entering last name...");
		clearAndSendKeys(driver.findElement(By.id("inputbox4")), nameParts[nameParts.length - 1]);
		System.out.println("Entering phone number...");
		clearAndSendKeys(driver.findElement(By.id("inputbox5")), order.buyer_phone_number == null ? VSDropShipper.VS_PHONE_NUM : order.buyer_phone_number);
		System.out.println("Entering street address...");
		clearAndSendKeys(driver.findElement(By.id("inputbox6")), order.buyer_street_address);

		clearAndSendKeys(driver.findElement(By.id("inputbox7")), "");
		if(order.buyer_apt_suite_unit_etc != null) {
			System.out.println("Entering apt / suite / bldg...");
			clearAndSendKeys(driver.findElement(By.id("inputbox7")), order.buyer_apt_suite_unit_etc);
		}

		System.out.println("Entering city...");
		clearAndSendKeys(driver.findElement(By.id("inputbox8")), order.buyer_city);

		System.out.println("Selecting state...");
		final WebElement stateBox = driver.findElement(By.cssSelector(".sc-payments-address-fields-state .sc-select-box"));
		stateBox.click();
		final List<WebElement> states = stateBox.findElements(By.cssSelector(".sc-select-dropdown-wrapper-open > .sc-select-options > .sc-select-option"));
		final String stateToSelect = StateUtils.getStateNameFromCode(order.buyer_state_province_region);

		for(final WebElement state : states) {
			if(state.getText().equalsIgnoreCase(stateToSelect)) {
				state.click();
				break;
			}
		}

		System.out.println("Entering zip code...");
		final String[] zipCodeParts = order.buyer_zip_postal_code.split("-");
		clearAndSendKeys(driver.findElement(By.id("inputbox9")), zipCodeParts[0]);

		System.out.println("Clicking save...");
		final WebElement saveButton = driver.findElement(By.cssSelector(".sc-edit-tile-edit-form-actions > .sc-btn-primary"));
		driver.scrollIntoView(saveButton);
		saveButton.click();
		Thread.sleep(2500); // wait for save
		System.out.println("\tAddress has been saved.");
	}

	private void scrollToTopOfPage() {
		final JavascriptExecutor jse = driver;
		jse.executeScript("window.scrollTo(0, 0)");
	}

	private void clearAndSendKeys(final WebElement el, final String str) throws InterruptedException {
		el.sendKeys(Keys.chord(Keys.CONTROL,"a", Keys.DELETE));
		Thread.sleep(50);
		el.sendKeys(str);
		Thread.sleep(50);
	}

	private void verifyListingTitle(final FulfillmentListing listing) throws InterruptedException {
		final Supplier<String> currentListingTitleSupp = () -> driver.findElement(By.className("sc-product-header-title-container")).getText();
		String currentListingTitle;
		startLoop();
		while((currentListingTitle = currentListingTitleSupp.get()).equals(lastWebPageTitle) && !hasExceededThreshold()) {
			Thread.sleep(10);
		}

		System.out.println("lastWebPageTitle: " + lastWebPageTitle + " --> " + currentListingTitle);
		lastWebPageTitle = currentListingTitle;
		if(!lastWebPageTitle.equalsIgnoreCase(listing.listing_title)) {
			throw new OrderExecutionException("Fulfillment listing title ("+currentListingTitle+") is not what we expected ("+listing.listing_title+")");
		}
	}

	private void enterQuantity(final CustomerOrder order, final WebElement orderOnlineBox) {
		final WebElement input = orderOnlineBox.findElement(By.id("inputbox2"));
		input.sendKeys(Integer.toString(order.fulfillment_purchase_quantity));
	}

	private void clickShipThisItem(final WebElement orderOnlineBox) {
		orderOnlineBox.findElement(By.className("sc-btn-primary")).click();
	}

	private void navigateToCart() {
		driver.get("https://www.samsclub.com/sams/cart/cart.jsp");
	}

	private void verifyCart(final CustomerOrder order, final FulfillmentListing listing) {
		//verify expected item quantity in cart
		final int numCartItems = Integer.parseInt(driver.findElement(By.cssSelector("#orderCount")).getText());
		if(numCartItems != order.fulfillment_purchase_quantity) {
			throw new OrderExecutionException("cart items != order fulfillment quantity: " + numCartItems + " != " + order.fulfillment_purchase_quantity);
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
