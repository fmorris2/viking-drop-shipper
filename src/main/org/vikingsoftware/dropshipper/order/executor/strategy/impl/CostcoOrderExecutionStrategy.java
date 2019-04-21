package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.util.List;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.AbstractOrderExecutionStrategy;

public class CostcoOrderExecutionStrategy extends AbstractOrderExecutionStrategy<CostcoWebDriver> {

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
			qty.clear();
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

	private WebElement narrowCartToTargetItem(final FulfillmentListing fulfillmentListing, final Supplier<List<WebElement>> orderItems) throws Exception {
		WebElement targetOrderItem = null;
		for(final WebElement item : orderItems.get()) {
			final String itemNum = item.getAttribute("data-orderitemnumber");
			if(!fulfillmentListing.item_id.equalsIgnoreCase(itemNum)) {
				System.out.println("Removing invalid item from cart: " + itemNum);
				final int oldNumCartItems = getNumCartItems();
				item.findElement(By.className("remove-link")).click();
				waitForCartUpdate(oldNumCartItems);
			} else {
				targetOrderItem = item;
			}
		}

		if(targetOrderItem == null) {
			throw new OrderExecutionException("Could not find target order item " + fulfillmentListing.item_id + " in cart");
		}

		return targetOrderItem;
	}

	private int getNumCartItems() {
		final WebElement subtotalBlock = driver.findElement(By.id("order-estimated-subtotal"));
		final Supplier<List<WebElement>> pullLeftEls = () -> subtotalBlock.findElements(By.className("pull-left"));
		for(final WebElement el : pullLeftEls.get()) {
			if(el.getText().contains("Items")) {
				return Integer.parseInt(el.getText().substring(1).split(" ")[0]);
			}
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
