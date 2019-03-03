package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriver;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.OrderTrackingHandler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.ebay.soap.eBLBaseComponents.ShipmentDeliveryStatusCodeType;

public class AliExpressOrderTrackingHandler implements OrderTrackingHandler {
	
	private static final int SELENIUM_INSTANCES_PER_CORE = 3;
	private static final String BASE_ORDER_DETAIL_URL = "http://trade.aliexpress.com/order_detail.htm?orderId=";
	
	private ExecutorService threadPool;
	private BlockingQueue<AliExpressDriverSupplier> webDrivers;
	
	@Override
	public boolean prepareToTrack() {
		final int coresAvailable = Runtime.getRuntime().availableProcessors();
		final int numThreads = coresAvailable * SELENIUM_INSTANCES_PER_CORE;
		System.out.println("Using " + numThreads + " threads for AliExpressOrderTrackingHandler");
		threadPool = Executors.newFixedThreadPool(numThreads);
		
		webDrivers = new ArrayBlockingQueue<>(numThreads);
		for(int i = 0; i < numThreads; i++) {
			try {
				webDrivers.put(new AliExpressDriverSupplier());
			} catch(final InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}

	@Override
	public Future<TrackingEntry> getTrackingInfo(final ProcessedOrder order) {
		return threadPool.submit(() -> getTrackingInfoImpl(order));
	}
	
	private TrackingEntry getTrackingInfoImpl(final ProcessedOrder order) {
		System.out.println("Initiating tracking info process for processed order " + order.id);
		AliExpressWebDriver driver = null;
		AliExpressDriverSupplier supplier = null;
		try {
			supplier = webDrivers.take();
			driver = supplier.get();
			if(driver.getReady()) {
				return parseTrackingInfo(driver, order);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			if(supplier != null) {
				webDrivers.add(supplier);
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
			final WebElement specificDetails = mostRecentDetails.findElements(By.className("tracks-list")).get(0);
			final List<WebElement> details = specificDetails.findElements(By.tagName("li"));
			final String mostRecentRemark = details.get(details.size() - 1).getText();
			final String status = statusCode == ShipmentDeliveryStatusCodeType.DELIVERED ? "complete" : "shipped";
			return new TrackingEntry(shippingCompany, trackingNum, status, mostRecentRemark, 
					statusCode);
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
		try {
			System.out.println("AliExpressOrderTrackingHandler#finishTracking...");
			threadPool.shutdownNow();
			webDrivers.forEach(driver -> driver.close());
			webDrivers.clear();
			threadPool = null;
			webDrivers = null;
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

}
