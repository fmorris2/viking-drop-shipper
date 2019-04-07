package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.OrderTrackingHandler;

public class SamsClubOrderTrackingHandler implements OrderTrackingHandler {

	private static final String BASE_ORDER_DETAILS_URL = "https://www.samsclub.com/sams/shoppingtools/orderhistory/orderDetailsPage.jsp?orderId=";
	private static final String EXTRA_PARAM = "&xid=account_order-details";

	@Override
	public boolean prepareToTrack() {
		return true;
	}

	@Override
	public RunnableFuture<TrackingEntry> getTrackingInfo(final ProcessedOrder order) {
		return new FutureTask<>(() -> getTrackingInfoImpl(order));
	}

	private TrackingEntry getTrackingInfoImpl(final ProcessedOrder order) {
		System.out.println("Initiating tracking info process for processed order " + order.id);
		SamsClubWebDriver driver = null;
		SamsClubDriverSupplier supplier = null;
		try {
			supplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
			driver = supplier.get();
			final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(order.fulfillment_account_id);
			if(driver.getReady(account)) {
				return parseTrackingInfo(driver, order);
			} else {
				return restart(order, supplier);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			if(supplier != null) {
				BrowserRepository.get().relinquish(supplier);
			}
		}
		return null;
	}

	private TrackingEntry restart(final ProcessedOrder order, final SamsClubDriverSupplier supplier) {
		supplier.get().quit();
		BrowserRepository.get().replace(supplier);
		return getTrackingInfoImpl(order);
	}

	private TrackingEntry parseTrackingInfo(final WebDriver driver, final ProcessedOrder order) {
		driver.get(BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id + EXTRA_PARAM);
		System.out.println("Navigating to page: " + BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id + EXTRA_PARAM);

		final String trackingNum = driver.findElement(By.cssSelector(".content-of-page > .brdrB > table td:first-child a[href*=\"tracking_numbers\"]")).getText();
		System.out.println("Tracking #: " + trackingNum);

		final Map<String, Integer> columnMappings = new HashMap<>();
		final List<WebElement> columns = driver.findElements(By.cssSelector(".content-of-page > .brdrB > table tr:first-child td"));
		for(int i = 0; i < columns.size(); i++) {
			columnMappings.put(columns.get(i).getText(), i+1);
		}

		final String orderStatus = driver.findElement(By
				.cssSelector(".content-of-page > .brdrB > table td:nth-child("+columnMappings.get("Status")+")"))
				.getText().trim();

		System.out.println("Order Status: " + orderStatus);


		return null;
	}

	@Override
	public void finishTracking() {
	}

}
