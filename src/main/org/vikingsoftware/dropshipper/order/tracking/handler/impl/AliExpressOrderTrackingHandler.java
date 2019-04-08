package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.ebay.soap.eBLBaseComponents.ShipmentDeliveryStatusCodeType;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.OrderTrackingHandler;

public class AliExpressOrderTrackingHandler implements OrderTrackingHandler {

	private static final String BASE_ORDER_DETAIL_URL = "http://trade.aliexpress.com/order_detail.htm?orderId=";

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
		AliExpressWebDriver driver = null;
		AliExpressDriverSupplier supplier = null;
		try {
			supplier = BrowserRepository.get().request(AliExpressDriverSupplier.class);
			driver = supplier.get();
			final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(order.fulfillment_account_id);
			if(driver.getReady(account)) {
				return parseTrackingInfo(driver, order);
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

	private TrackingEntry parseTrackingInfo(final WebDriver driver, final ProcessedOrder order) {
		driver.get(BASE_ORDER_DETAIL_URL + order.fulfillment_transaction_id);
		final List<WebElement> shippingDetailBlocks = driver.findElements(By.className("shipping-bd"));
		if(!shippingDetailBlocks.isEmpty()) {
			final WebElement stepsEl = driver.findElement(By.className("ui-step"));
			final WebElement currentStepEl = stepsEl.findElement(By.className("current"));
			final ShipmentDeliveryStatusCodeType statusCode = getStatusCode(currentStepEl.getText().trim());
			final WebElement mostRecentDetails = shippingDetailBlocks.get(shippingDetailBlocks.size() - 1);
			final String shippingCompany = mostRecentDetails.findElement(By.className("logistics-name")).getText();
			final String trackingNum = mostRecentDetails.findElement(By.className("no")).getText();
			return new TrackingEntry(shippingCompany, trackingNum, statusCode);
		}
		return null;
	}

	private ShipmentDeliveryStatusCodeType getStatusCode(final String text) {
		final String lower = text.toLowerCase();
		if(lower.contains("pay success")) {
			return ShipmentDeliveryStatusCodeType.CREATED;
		}

		if(lower.contains("shipment")) {
			return ShipmentDeliveryStatusCodeType.IN_TRANSIT;
		}

		if(lower.contains("order complete")) {
			return ShipmentDeliveryStatusCodeType.DELIVERED;
		}

		return ShipmentDeliveryStatusCodeType.UNKNOWN;
	}

	@Override
	public void finishTracking() {

	}

}
