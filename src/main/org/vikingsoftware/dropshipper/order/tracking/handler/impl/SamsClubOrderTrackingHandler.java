package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.ebay.soap.eBLBaseComponents.ShipmentDeliveryStatusCodeType;

import main.mysterytracking.TrackingNumber;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.order.tracking.error.UnknownTrackingIdException;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.AbstractOrderTrackingHandler;

public class SamsClubOrderTrackingHandler extends AbstractOrderTrackingHandler<SamsClubWebDriver> {

	private static final String BASE_ORDER_DETAILS_URL = "https://www.samsclub.com/sams/shoppingtools/orderhistory/orderDetailsPage.jsp?orderId=";
	private static final String EXTRA_PARAM = "&xid=account_order-details";

	@Override
	public boolean prepareToTrack() {
		return true;
	}

	@Override
	protected TrackingEntry parseTrackingInfo(final SamsClubWebDriver driver, final ProcessedOrder order) {
		driver.get(BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id + EXTRA_PARAM);
		System.out.println("Navigating to page: " + BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id + EXTRA_PARAM);

		final WebElement trackingNumEl = driver.findElements(By.tagName("a")).stream().filter(el -> {
			final String href = el.getAttribute("href");
			if(href != null) {
				return href.contains("tracking/tracking.htm?trackingId=")
						|| href.contains("fedex?tracking_numbers=");
			}

			return false;
		}).findFirst().orElse(null);

		String trackingNum = null;
		if(trackingNumEl != null) {
			trackingNum = trackingNumEl.getText();
		} else {
			throw new RuntimeException("Could not parse tracking number from page for order " + order.id);
		}

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
	protected Class<? extends DriverSupplier<SamsClubWebDriver>> getDriverSupplierClass() {
		return SamsClubDriverSupplier.class;
	}

}
