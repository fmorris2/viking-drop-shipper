package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.util.List;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class CostcoOrderExecutionStrategy extends AbstractOrderExecutionStrategy<CostcoWebDriver> {

	private static final String EMAIL = "wholesale@vikingsoftware.org";
	private static final String TOKEN_CVV = "983";

	@Override
	protected Class<CostcoDriverSupplier> getDriverSupplierClass() {
		return CostcoDriverSupplier.class;
	}

	@Override
	protected ProcessedOrder executeOrderImpl(CustomerOrder order, FulfillmentListing fulfillmentListing)
			throws Exception {

		driver.get(fulfillmentListing.listing_url);

		enterQuantity(order);
		addToCart();
		waitForAddedToCartModal();

		goToCart();
		verifyCart(order, fulfillmentListing);
		clickCheckout();

		enterShippingDetails(order, fulfillmentListing);
		useSelectedAddress();
		verifyPriceOnShippingPage(order, fulfillmentListing);
		clickContinueToPayment();
		enterCVV();
		clickContinueToReviewOrder();
		return null;
	}

	private void enterQuantity(final CustomerOrder order) {
		final WebElement qtyBox = driver.findElement(By.id("minQtyText"));
		qtyBox.clear();
		qtyBox.sendKeys(Integer.toString(order.fulfillment_purchase_quantity));
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

	private void clickCheckout() {
		driver.findElement(By.id("shopCartCheckoutSubmitButton")).click();
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
		driver.findElement(By.cssSelector(".address-choose-header a")).click();

		System.out.println("Entering buyer first name: " + order.getFirstName());
		driver.findElement(By.id("firstId")).sendKeys(order.getFirstName());

		System.out.println("Entering buyer last name: " + order.getLastName());
		driver.findElement(By.id("lastId")).sendKeys(order.getLastName());

		System.out.println("Entering buyer street address: " + order.buyer_street_address);
		driver.findElement(By.id("address1Id")).sendKeys(order.buyer_street_address);

		System.out.println("Entering buyer apt / suite / unit / etc: " + order.buyer_apt_suite_unit_etc);
		driver.findElement(By.id("address2Id")).sendKeys(order.buyer_apt_suite_unit_etc == null ? "" : order.buyer_apt_suite_unit_etc);

		System.out.println("Entering buyer zip code: " + order.buyer_zip_postal_code.substring(0, 5));
		driver.executeScript("document.getElementById('postalId').setAttribute('value', '"+order.buyer_zip_postal_code.subSequence(0, 5)+"')");
		driver.findElement(By.id("postalId")).sendKeys(Keys.SPACE, Keys.BACK_SPACE);

		Thread.sleep(15000);

		final String phoneNum = order.buyer_phone_number == null ? null : order.buyer_phone_number.replaceAll("\\D", "");
		if(phoneNum != null) {
			System.out.println("Entering buyer phone number: " + phoneNum);
			driver.executeScript("document.getElementById('phoneId').setAttribute('value', '"+phoneNum+"')");
		}

		System.out.println("Entering email: " + EMAIL);
		driver.findElement(By.id("emailId")).sendKeys(EMAIL);

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
		driver.setImplicitWait(30);
		try {
			driver.findElement(By.cssSelector("#entered-address input")).click();
			driver.findElement(By.id("costcoModalBtn2")).click();
			clickContinueToPayment();
		} finally {
			driver.resetImplicitWait();
		}
	}

	private void enterCVV() {
		System.out.println("Entering CVV...");
		driver.findElement(By.cssSelector("#cc_cvv_div iframe")).sendKeys(TOKEN_CVV);
	}

	private void clickContinueToReviewOrder() {
		System.out.println("Clicking continue to review order...");
		driver.findElement(By.cssSelector("#order-summary-body input[name=\"place-order\"]")).click();
	}

	private WebElement narrowCartToTargetItem(final FulfillmentListing fulfillmentListing, final Supplier<List<WebElement>> orderItems) throws Exception {
		for(final WebElement item : orderItems.get()) {
			final String itemNum = item.getAttribute("data-orderitemnumber");
			if(!fulfillmentListing.item_id.equalsIgnoreCase(itemNum)) {
				System.out.println("Removing invalid item from cart: " + itemNum);
				final int oldNumCartItems = getNumCartItems();
				try {
					item.findElement(By.className("remove-link")).click();
				} catch(final WebDriverException e) {
					if(!(e instanceof NoSuchElementException)) {
						Thread.sleep(1000);
						System.out.println("Failed to click remove link... Retrying");
						return narrowCartToTargetItem(fulfillmentListing, orderItems);
					}

				}
				waitForCartUpdate(oldNumCartItems);
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
				if(el.getText().contains("Items")) {
					return Integer.parseInt(el.getText().substring(1).split(" ")[0]);
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
