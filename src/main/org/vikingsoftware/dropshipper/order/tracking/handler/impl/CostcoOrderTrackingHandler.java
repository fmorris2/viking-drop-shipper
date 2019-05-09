package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.ebay.soap.eBLBaseComponents.ShipmentDeliveryStatusCodeType;

import main.mysterytracking.TrackingNumber;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.order.tracking.error.UnknownTrackingIdException;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.AbstractOrderTrackingHandler;

public class CostcoOrderTrackingHandler extends AbstractOrderTrackingHandler<CostcoWebDriver> {

	private static final String BASE_ORDER_DETAILS_URL = "https://www.costco.com/OrderStatusDetailsView?orderId=";

	private static final String TRACKING_NUM_SELECTOR = ".shipment_details .blue_link";

	@Override
	public boolean prepareToTrack() {
		return true;
	}

	@Override
	protected TrackingEntry parseTrackingInfo(CostcoWebDriver driver, ProcessedOrder order) {
		driver.get(BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id);
		final WebElement trackingNumEl = driver.findElement(By.cssSelector(TRACKING_NUM_SELECTOR));
		final String trackingNum = trackingNumEl.getText();
		final TrackingNumber carrierDetails = TrackingNumber.parse(trackingNum);
		if(!carrierDetails.isCourierRecognized()) {
			throw new UnknownTrackingIdException("Unknown courier for tracking number: " + trackingNum);
		}

		final String courierName = carrierDetails.getCourierName().replaceAll("[^a-zA-Z0-9\\-\\s]", "");
		System.out.println("Tracking #: " + trackingNum);
		System.out.println("Carrier: " + courierName);
		return new TrackingEntry(courierName, trackingNum, ShipmentDeliveryStatusCodeType.IN_TRANSIT);
	}

	@Override
	public void finishTracking() {
	}

	@Override
	protected Class<? extends DriverSupplier<CostcoWebDriver>> getDriverSupplierClass() {
		return CostcoDriverSupplier.class;
	}

}
