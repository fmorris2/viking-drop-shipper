package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class CostcoOrderExecutionStrategy extends AbstractOrderExecutionStrategy<CostcoWebDriver> {

	private static final String EMAIL = "wholesale@vikingsoftware.org";
	private static final String TOKEN_CVV = "983";

	private int enterQtyFailures = 0;

	@Override
	protected Class<CostcoDriverSupplier> getDriverSupplierClass() {
		return CostcoDriverSupplier.class;
	}

	@Override
	protected ProcessedOrder executeOrderImpl(CustomerOrder order, FulfillmentListing fulfillmentListing)
			throws Exception {

		enterQtyFailures = 0;
		System.out.println("Loading fulfillment URL: " + fulfillmentListing.listing_url);
		driver.get(fulfillmentListing.listing_url);
		System.out.println("\tdone.");

		System.out.println("Entering quantity...");
		enterQuantity(order);
		System.out.println("\tdone.");
		addToCart();
		waitForAddedToCartModal();

		goToCart();
		verifyCart(order, fulfillmentListing);
		clickCheckout();

		enterShippingDetails(order, fulfillmentListing);
		useSelectedAddress();
		verifyPriceOnShippingPage(order, fulfillmentListing);
		clickContinueToPayment();
		try {
			enterCVV();
		} catch(final Exception e) {
			System.out.println("No need to enter CVV");
		}
		clickContinueToReviewOrder();
		return verifyAndPlaceOrder(order, fulfillmentListing);
	}

	private void enterQuantity(final CustomerOrder order) throws InterruptedException {
		try {
			final WebElement qtyBox = driver.findElement(By.id("minQtyText"));
			qtyBox.clear();
			qtyBox.sendKeys(Integer.toString(order.fulfillment_purchase_quantity));
		} catch(final ElementNotVisibleException e) {
			if(enterQtyFailures > 30) {
				throw new OrderExecutionException("Failed to enter qty");
			}
			Thread.sleep(1000);
			enterQtyFailures++;
			enterQuantity(order);
		}
	}

	private void addToCart() {
		driver.findElement(By.id("add-to-cart-btn")).click();
	}

	private void waitForAddedToCartModal() throws InterruptedException {
		driver.setImplicitWait(30);
		try {
			final WebElement modal = driver.findElement(By.id("costcoModalTitle"));
			startLoop();
			while(modal.getText().isEmpty() && !hasExceededThreshold()) {
				Thread.sleep(50);
			}

			if(!modal.getText().equalsIgnoreCase("added to cart")) {
				throw new OrderExecutionException("Costco 'added to cart modal' never showed up!");
			}
		} finally {
			driver.resetImplicitWait();
		}
	}

	private void goToCart() {
		driver.get("https://www.costco.com/CheckoutCartView");
	}

	private void verifyCart(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws Exception {
		final Supplier<List<WebElement>> orderItems = () -> driver.findElements(By.className("order-item"));
		final WebElement targetOrderItem = narrowCartToTargetItem(fulfillmentListing, orderItems);

		System.out.println("Target order item has been identified for item id " + fulfillmentListing.item_id);

		adjustAndVerifyQty(order, fulfillmentListing, targetOrderItem);
		verifyEstimatedOrderTotal(order, fulfillmentListing);
	}

	private void adjustAndVerifyQty(final CustomerOrder order, final FulfillmentListing fulfillmentListing,
			final WebElement targetOrderItem) throws Exception {
		final WebElement qty = targetOrderItem.findElement(By.id("quantity_1"));
		if(!qty.getAttribute("value").equals(Integer.toString(order.fulfillment_purchase_quantity))) {
			System.out.println("Cart quantity for " + fulfillmentListing.item_id + " is not correct. Editing...");
			try {
				qty.clear();
			} catch(final InvalidElementStateException e) {
				System.out.println("Qty clear button is not interactable. Retrying...");
				Thread.sleep(1000);
				adjustAndVerifyQty(order, fulfillmentListing, targetOrderItem);
				return;
			}
			qty.sendKeys(Integer.toString(order.fulfillment_purchase_quantity));
			final int oldQty = getNumCartItems();
			qty.sendKeys(Keys.TAB);
			waitForCartUpdate(oldQty);
		}

		if(getNumCartItems() != order.fulfillment_purchase_quantity) {
			throw new OrderExecutionException("Inconsistency with cart qty");
		}
	}

	private void verifyEstimatedOrderTotal(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		final WebElement estimatedTotalEl = driver.findElement(By.id("order-estimated-total"));
		final String estimatedTotalTxt = estimatedTotalEl.getText().substring(1).trim();
		final double estimatedTotal = Double.parseDouble(estimatedTotalTxt);
		if(order.getProfit(estimatedTotal) < 0) {
			throw new OrderExecutionException("WARNING: POTENTIAL FULFILLMENT AT LOSS for fulfillment listing " + fulfillmentListing.id
					+ "! PROFIT: $" + order.getProfit(estimatedTotal));
		}

		System.out.println("Estimated order total has been verified in the cart.");
	}

	private void clickCheckout() throws InterruptedException {
		try {
			driver.findElement(By.id("shopCartCheckoutSubmitButton")).click();
		} catch(final ElementClickInterceptedException e) {
			e.printStackTrace();
			Thread.sleep(1000);
			clickCheckout();
			return;
		}
		driver.setImplicitWait(30);
		try {
			System.out.println("Waiting for checkout shipping screen to appear");
			driver.findElement(By.id("shipping"));
		} finally {
			driver.resetImplicitWait();
		}
	}

	private void enterShippingDetails(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		System.out.println("Clicking change address...");
		driver.setImplicitWait(30);
		try {
			driver.findElement(By.cssSelector("#address-block-shipping > .right > a")).click();
		} catch(final WebDriverException e) {
			if(!(e instanceof NoSuchElementException)) {
				Thread.sleep(1000);
				System.out.println("Could not click change address... retrying.");
				enterShippingDetails(order, fulfillmentListing);
				return;
			}
		}
		System.out.println("Clicking add new address");
		Thread.sleep(1000); //wait for modal to pop up
		driver.findElement(By.cssSelector(".address-choose-header a")).click();

		System.out.println("Entering buyer first name: " + order.getFirstName());
		final WebElement firstNameEl = driver.findElement(By.id("firstId")); //wait for modal to appear
		firstNameEl.clear();
		Thread.sleep(500);
		driver.sendKeysSlowly(firstNameEl, order.getFirstName());
		Thread.sleep(500);

		System.out.println("Entering buyer last name: " + order.getLastName());
		final WebElement lastNameEl = driver.findElement(By.id("lastId"));
		lastNameEl.clear();
		Thread.sleep(500);
		driver.sendKeysSlowly(lastNameEl, order.getLastName());
		Thread.sleep(500);

		System.out.println("Entering buyer street address: " + order.buyer_street_address);
		final WebElement streetEl = driver.findElement(By.id("address1Id"));
		streetEl.clear();
		Thread.sleep(500);
		driver.sendKeysSlowly(streetEl, order.buyer_street_address);
		Thread.sleep(500);

		final WebElement address2 = driver.findElement(By.id("address2Id"));
		address2.clear();
		Thread.sleep(500);
		if(order.buyer_apt_suite_unit_etc != null && !order.buyer_apt_suite_unit_etc.isEmpty()) {
			System.out.println("Entering buyer apt / suite / unit / etc: " + order.buyer_apt_suite_unit_etc);
			driver.sendKeysSlowly(address2, order.buyer_apt_suite_unit_etc);
			Thread.sleep(500);
		}

		System.out.println("Entering buyer zip code: " + order.buyer_zip_postal_code.substring(0, 5));
		final WebElement zipEl = driver.findElement(By.id("postalId"));
		zipEl.clear();
		Thread.sleep(500);
		driver.sendKeysSlowly(zipEl, order.buyer_zip_postal_code.substring(0, 5));
		Thread.sleep(500);

		final String phoneNum = order.buyer_phone_number == null ? null : order.buyer_phone_number.replaceAll("\\D", "");
		if(phoneNum != null) {
			System.out.println("Entering buyer phone number: " + phoneNum);
			final WebElement phoneNumEl = driver.findElement(By.id("phoneId"));
			phoneNumEl.clear();
			Thread.sleep(500);
			driver.sendKeysSlowly(phoneNumEl, phoneNum);
			Thread.sleep(500);
		}

		System.out.println("Entering city: " + order.buyer_city);
		final WebElement cityEl = driver.findElement(By.id("cityId"));
		cityEl.clear();
		Thread.sleep(500);
		driver.sendKeysSlowly(cityEl, order.buyer_city);
		Thread.sleep(500);

		System.out.println("Entering email: " + EMAIL);
		final WebElement emailEl = driver.findElement(By.id("emailId"));
		emailEl.clear();
		Thread.sleep(500);
		driver.sendKeysSlowly(emailEl, EMAIL);

		System.out.println("Unchecking save address box");
		driver.executeScript("document.getElementById('save-address-modal').checked = false");
		driver.executeScript("document.getElementById('set-default-modal').checked = false");

		driver.findElement(By.id("costcoModalBtn2")).click();
	}

	private void verifyPriceOnShippingPage(final CustomerOrder order, final FulfillmentListing fulfillmentListing) {
		System.out.println("Verifying price on shipping page...");
		final WebElement priceEl = driver.findElement(By.id("outstandingPrincipal"));
		final double price = Double.parseDouble(priceEl.getAttribute("value"));

		if(order.getProfit(price) < 0) {
			throw new OrderExecutionException("WARNING: POTENTIAL FULFILLMENT AT LOSS for fulfillment listing " + fulfillmentListing.id
					+ "! PROFIT: $" + order.getProfit(price));
		}
		System.out.println("\tVerified.");
	}

	private void clickContinueToPayment() throws InterruptedException {
		System.out.println("Continuing to payment...");
		try {
			driver.findElement(By.cssSelector(".big-green[name=\"place-order\"]")).click();
		} catch(final WebDriverException e) {
			if(!(e instanceof NoSuchElementException)) {
				Thread.sleep(1000);
				System.out.println("Could not click continue to payment. Retrying...");
				clickContinueToPayment();
			}
		}
	}

	private void useSelectedAddress() throws InterruptedException {
		driver.setImplicitWait(60);
		try {
			driver.findElement(By.cssSelector("#entered-address input")).click();
			driver.findElement(By.id("costcoModalBtn2")).click();
		} finally {
			driver.resetImplicitWait();
		}
	}

	private void enterCVV() throws InterruptedException {
		Thread.sleep(3000);
		System.out.println("Entering CVV...");
		driver.findElement(By.cssSelector("#cc_cvv_div iframe")).sendKeys(TOKEN_CVV);
	}

	private void clickContinueToReviewOrder() {
		System.out.println("Clicking continue to review order...");
		driver.findElement(By.cssSelector("#order-summary-body input[name=\"place-order\"]")).click();
	}

	private ProcessedOrder verifyAndPlaceOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		driver.setImplicitWait(30);
		final ProcessedOrder.Builder builder = new ProcessedOrder.Builder()
				.customer_order_id(order.id)
				.fulfillment_account_id(account.id)
				.fulfillment_listing_id(fulfillmentListing.id);

		verifyShippingInfo(order);
		System.out.println("Shipping info has been verified...");

		verifyFinancials(order, fulfillmentListing, builder);
		System.out.println("Financials have been verified...");

		System.out.println("Verifications have been completed - Placing order...");
		return placeOrder(builder);
	}

	private void verifyShippingInfo(final CustomerOrder order) {
		List<WebElement> addressElements = driver.findElements(By.cssSelector("#address-block-shipping > .address-display .ctHidden"));

		this.startLoop();
		while(addressElements.isEmpty() && !this.hasExceededThreshold()) {
			addressElements = driver.findElements(By.cssSelector("#address-block-shipping > .address-display .ctHidden"));
		}

		String shippingInfo = "";
		for(final WebElement el : addressElements) {
			shippingInfo += el.getText().toLowerCase().trim() + " ";
		}

		if(!shippingInfo.contains(order.getFirstName().toLowerCase())) {
			throw new OrderExecutionException("Shipping info does not contain first name: " + order.getFirstName() + " is not in (" + shippingInfo + ")");
		}

		if(!shippingInfo.contains(order.getLastName().toLowerCase())) {
			throw new OrderExecutionException("Shipping info does not contain last name: " + order.getLastName() + " is not in (" + shippingInfo + ")");
		}

		if(!shippingInfo.contains(order.buyer_street_address.toLowerCase())) {
			throw new OrderExecutionException("Shipping info does not contain buyer street address: " + order.buyer_street_address + " is not in (" + shippingInfo + ")");
		}

		if(!shippingInfo.contains(order.buyer_city.toLowerCase())) {
			throw new OrderExecutionException("Shipping info does not contain buyer city: " + order.buyer_city + " is not in (" + shippingInfo + ")");
		}

		if(!shippingInfo.contains(order.buyer_state_province_region.toLowerCase())) {
			throw new OrderExecutionException("Shipping info does not contain buyer state: " + order.buyer_state_province_region + " is not in (" + shippingInfo + ")");
		}

		if(!shippingInfo.contains(order.buyer_zip_postal_code.substring(0, 5).toLowerCase())) {
			throw new OrderExecutionException("Shipping info does not contain buyer zip: " + order.buyer_zip_postal_code + " is not in (" + shippingInfo + ")");
		}
	}

	private void verifyFinancials(final CustomerOrder order, final FulfillmentListing listing, final ProcessedOrder.Builder builder) {
		final double totalPrice = Double.parseDouble(driver.findElement(By.id("outstandingPrincipal")).getAttribute("value"));
		System.out.println("total price: " + totalPrice);
		final double profit = order.getProfit(totalPrice);
		System.out.println("profit: " + profit);

		if(profit < 0) {
			throw new OrderExecutionException("WARNING: POTENTIAL FULFILLMENT AT LOSS for fulfillment listing " + listing.id
					+ "! PROFIT: $" + order.getProfit(profit));
		}

		final WebElement orderSummaryEl = driver.findElement(By.id("order-summary-body"));
		final WebElement detailsBlock = orderSummaryEl.findElement(By.className("dl-horizontal"));
		final List<WebElement> dtEls = detailsBlock.findElements(By.tagName("dt"));
		final List<WebElement> ddEls = detailsBlock.findElements(By.tagName("dd"));
		for(int i = 0; i < dtEls.size(); i++) {
			final String title = dtEls.get(i).getText().toLowerCase();
			if(title.contains("subtotal")) {
				final double subtotal = Double.parseDouble(ddEls.get(i).getText().substring(1));
				System.out.println("subtotal: " + subtotal);
				builder.buy_subtotal(subtotal);
			} else if(title.contains("shipping")) {
				final double shipping = Double.parseDouble(ddEls.get(i).getText().substring(1));
				System.out.println("shipping: " + shipping);
				builder.buy_shipping(shipping);
			} else if(title.contains("fee")) {
				final double fee = Double.parseDouble(ddEls.get(i).getText().substring(1));
				System.out.println("fee: " + fee);
				builder.buy_product_fees(fee);
			} else if(title.contains("tax")) {
				final double tax = Double.parseDouble(ddEls.get(i).getText().substring(1));
				System.out.println("tax: " + tax);
				builder.buy_sales_tax(tax);
			}
		}

		builder
			.buy_total(totalPrice)
			.profit(profit);
	}

	private ProcessedOrder placeOrder(final ProcessedOrder.Builder builder) throws InterruptedException {
		final WebElement placeOrderButton = driver.findElement(By.cssSelector("input[name=\"place-order\"]"));
		driver.scrollIntoView(placeOrderButton);
		try {
			Thread.sleep(1000);
			if(!OrderExecutor.isTestMode) {
				placeOrderButton.click();
			}
		} catch(final WebDriverException e) {
			e.printStackTrace();
			Thread.sleep(1500);
			return placeOrder(builder);
		}
		driver.setImplicitWait(90);
		
		try(final FileWriter fW = new FileWriter("last-order-page.html");
			final BufferedWriter bW = new BufferedWriter(fW);) {
			bW.write(driver.getPageSource());
			bW.flush();
		} catch(final Exception e) {
			//swallow
		}

		//parse transaction id....
		try {
			final String transactionId = driver.findElement(
				By.cssSelector("#order-confirmation-body > div:nth-child(2) > div > div:nth-child(2) > p:nth-child(2)")).getText();
			System.out.println("transactionId: " + transactionId);
			builder.fulfillment_transaction_id(transactionId);
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.critical(getClass(), "failed to submit order: " + builder.build(), e);
			//THIS IS VERY VERY BAD!!! COSTCO MIGHT HAVE CHANGED THEIR FRONT END? WE SHOULD NO LONGER PROCESS ORDERS
			//AND WE SHOULD NOTIFY DEVELOPERS IMMEDIATELY
			FulfillmentManager.freeze(FulfillmentPlatforms.COSTCO.getId());
			System.out.println("Submitted an order, but we failed to parse whether it was a success or not. Freezing orders...");
		}

		return builder.build();
	}

	private WebElement narrowCartToTargetItem(final FulfillmentListing fulfillmentListing, final Supplier<List<WebElement>> orderItems) throws Exception {
		startLoop();
		while(orderItems.get().isEmpty() && !this.hasExceededThreshold()) {
			Thread.sleep(50);
		}

		System.out.println("# order items: " + orderItems.get().size());

		long numCorrectItems = orderItems.get()
				.stream()
				.filter(el -> el.getAttribute("data-orderitemnumber").equalsIgnoreCase(fulfillmentListing.item_id))
				.count();

		for(final WebElement item : orderItems.get()) {
			final String itemNum = item.getAttribute("data-orderitemnumber");
			final boolean isItem = fulfillmentListing.item_id.equalsIgnoreCase(itemNum);
			if(!isItem || numCorrectItems > 1) {
				System.out.println("Removing invalid item from cart: " + itemNum);
				final int oldNumCartItems = getNumCartItems();
				try {
					item.findElement(By.className("remove-link")).findElement(By.tagName("a")).click();
				} catch(final WebDriverException e) {
					if(!(e instanceof NoSuchElementException)) {
						Thread.sleep(1000);
						System.out.println("Failed to click remove link... Retrying");
						return narrowCartToTargetItem(fulfillmentListing, orderItems);
					}

				}
				waitForCartUpdate(oldNumCartItems);
				if(isItem) {
					numCorrectItems--;
				}
			}
		}

		final List<WebElement> items = orderItems.get();

		final WebElement targetOrderItem = items.size() == 1 ? items.get(0) : null;

		if(targetOrderItem == null) {
			throw new OrderExecutionException("Could not find target order item " + fulfillmentListing.item_id + " in cart");
		}

		return targetOrderItem;
	}

	private int getNumCartItems() {
		final Supplier<WebElement> subtotalBlock = () -> driver.findElement(By.id("order-estimated-subtotal"));
		final Supplier<List<WebElement>> pullLeftEls = () -> subtotalBlock.get().findElements(By.className("pull-left"));
		try {
			for(final WebElement el : pullLeftEls.get()) {
				if(el.getText().contains("Item")) {
					return Integer.parseInt(el.getText().substring(1).split(" ")[0]);
				}
			}
		} catch(final StaleElementReferenceException e) {
			e.printStackTrace();
			return getNumCartItems();
		}

		return -1;
	}

	private void waitForCartUpdate(final int oldNumCartItems) throws Exception {
		startLoop();
		while(oldNumCartItems == getNumCartItems()) {
			if(hasExceededThreshold()) {
				throw new OrderExecutionException("Failed to wait for cart update!");
			}
			Thread.sleep(10);
		}
	}

}
