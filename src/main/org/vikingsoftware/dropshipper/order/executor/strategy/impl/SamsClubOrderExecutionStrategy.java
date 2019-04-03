package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.error.OrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

public class SamsClubOrderExecutionStrategy implements OrderExecutionStrategy {

	private ProcessedOrder processedOrder;

	private DriverSupplier<SamsClubWebDriver> driverSupplier;
	private SamsClubWebDriver driver;

	private String lastWebPageTitle = "";

	@Override
	public boolean prepareForExecution() {
		System.out.println("SamsClubOrderExecutionStrategy#prepareForExecution");
		driverSupplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		return true;
	}

	@Override
	public ProcessedOrder order(CustomerOrder order, FulfillmentListing fulfillmentListing) {
		processedOrder = new ProcessedOrder.Builder()
				.customer_order_id(order.id)
				.fulfillment_listing_id(fulfillmentListing.id)
				.build();
			try {
				return executeOrder(order, fulfillmentListing);
			} catch(final Exception e) {
				e.printStackTrace();
				DBLogging.high(getClass(), "order failed: ", e);
			}

		return processedOrder;
	}

	private ProcessedOrder executeOrder(final CustomerOrder order, final FulfillmentListing fulfillmentListing) throws InterruptedException {
		System.out.println("SamsClubOrderExecutionStrategy#executeOrder");
		driver = driverSupplier.get();
		if(driver.getReady()) {
			System.out.println("\tSuccessfully prepared sams club driver");
			driver.get(fulfillmentListing.listing_url);

			verifyListingTitle(fulfillmentListing);
			verifyListingPrice(order, fulfillmentListing);

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

			System.out.println("Initial details verified. Beginning checkout process...");

		} else {
			System.out.println("\tFailed to prepare sams club driver! Attempting to restart.");
			return restart(order, fulfillmentListing);
		}
		return processedOrder;
	}

	private void verifyListingTitle(final FulfillmentListing listing) throws InterruptedException {
		final Supplier<String> currentListingTitleSupp = () -> driver.findElement(By.className("sc-product-header-title-container")).getText();
		String currentListingTitle;
		while((currentListingTitle = currentListingTitleSupp.get()).equals(lastWebPageTitle)) {
			Thread.sleep(10);
		}

		System.out.println("lastWebPageTitle: " + lastWebPageTitle + " --> " + currentListingTitle);
		lastWebPageTitle = currentListingTitle;
		if(!lastWebPageTitle.equalsIgnoreCase(listing.listing_title)) {
			throw new OrderExecutionException("Fulfillment listing title ("+currentListingTitle+") is not what we expected ("+listing.listing_title+")");
		}
	}

	/*
	 * Ensure we don't fulfill at a loss without manually doing so.
	 * We never want the system to automatically fulfill at a loss.
	 */
	private void verifyListingPrice(final CustomerOrder order, final FulfillmentListing listing) {
		final double customerOrderPrice = (order.sale_price / order.quantity);
		final String dollars = driver.findElement(By.className("Price-characteristic")).getText();
		final String cents = driver.findElement(By.className("Price-mantissa")).getText();
		final double currentFulfillmentPrice = Double.parseDouble(dollars + "." + cents);
		if(customerOrderPrice < currentFulfillmentPrice) {
			throw new OrderExecutionException("Fulfillment @ loss warning: " + customerOrderPrice + " - " + currentFulfillmentPrice);
		}
	}

	private void enterQuantity(final CustomerOrder order, final WebElement orderOnlineBox) {
		final WebElement input = orderOnlineBox.findElement(By.id("inputbox2"));
		input.sendKeys(Integer.toString(order.quantity));
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
		if(numCartItems != order.quantity) {
			throw new OrderExecutionException("cart items != order quantity: " + numCartItems + " != " + order.quantity);
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
		if(!listing.listing_id.equals(itemNumStrParts[itemNumStrParts.length - 1])) {
			throw new OrderExecutionException("Wrong item ID in cart: " + itemNumStrParts[itemNumStrParts.length - 1] + " != " + listing.listing_id);
		}

		//verify "ship it" option is picked
		final List<WebElement> deliveryOptions = itemRow.findElements(By.className("nc-delivery"));
		boolean isShipItSelected = false;
		for(final WebElement option : deliveryOptions) {
			final WebElement input = option.findElement(By.tagName("input"));
			final String value = input.getAttribute("value");
			if(value != null && value.equalsIgnoreCase("online") && input.getAttribute("checked") != null) {
				isShipItSelected = true;
				break;
			}
		}

		if(!isShipItSelected) {
			throw new OrderExecutionException("'Ship it' option is not selected!");
		}

		//verify price!
		final double total = Double.parseDouble(driver.findElement(By.id("nc-v2-est-total")).getText().substring(1));
		if(total > order.sale_price) { //never automatically sell at a loss....
			throw new OrderExecutionException("fulfillment order total is more than customer order sale price! " + total + " > " + order.sale_price);
		}

	}

	private void clearCart() {
		try {
			final WebElement parentCartTable = driver.findElement(By.className("cart-table"));
			final WebElement cartTable = parentCartTable.findElement(By.tagName("tbody"));
			final List<WebElement> removeEls = cartTable.findElements(By.className("js_remove"));

			System.out.println("clearing cart...");
			final Supplier<Integer> numCartItemsSupp = () -> Integer.parseInt(driver.findElement(By.cssSelector("#orderCount")).getText());
			int currentNumCartItems = numCartItemsSupp.get();
			for(final WebElement el : removeEls) {
				el.click();
				while(currentNumCartItems == numCartItemsSupp.get()) {
					Thread.sleep(10);
				}

				currentNumCartItems = numCartItemsSupp.get();
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	private ProcessedOrder restart(final CustomerOrder order, final FulfillmentListing listing) throws InterruptedException {
		driver.quit();
		BrowserRepository.get().replace(driverSupplier);
		driverSupplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
		return executeOrder(order, listing);
	}

	@Override
	public void finishExecution() {
		BrowserRepository.get().relinquish(driverSupplier);
		driver = null;
		driverSupplier = null;
	}

}
