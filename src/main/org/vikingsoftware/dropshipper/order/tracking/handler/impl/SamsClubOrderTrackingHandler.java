package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final String BASE_ORDER_DETAILS_URL = "https://www.samsclub.com/order/details/";
	private static final String TRACKING_NUM_PATTERN_STR = "tracking.+=(.+)";
	private static final Pattern TRACKING_NUM_PATTERN = Pattern.compile(TRACKING_NUM_PATTERN_STR);

	@Override
	public boolean prepareToTrack() {
		return true;
	}

	@Override
	protected TrackingEntry parseTrackingInfo(final SamsClubWebDriver driver, final ProcessedOrder order) {
		driver.get(BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id);
		System.out.println("Navigating to page: " + BASE_ORDER_DETAILS_URL + order.fulfillment_transaction_id);
		driver.savePageSource();

		driver.setImplicitWait(1);
		final WebElement shippedEl = driver.findElement(By.className("sc-account-online-order-details-tracking-info"));
		final WebElement trackingNumEl = shippedEl.findElements(By.cssSelector("a[href*=\"samsclub.com/tracking\"")).stream().filter(el -> {
			try {
				final String href = el.getAttribute("href");
				if(href != null) {
					return href.contains("tracking/tracking.htm?trackingId=")
							|| href.contains("fedex?tracking_numbers=");
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}

			return false;
		}).findFirst().orElse(null);
		driver.resetImplicitWait();

		String trackingNum = null;
		if(trackingNumEl != null) {
			final String href = trackingNumEl.getAttribute("href");
			final Matcher matcher = TRACKING_NUM_PATTERN.matcher(href);
			System.out.println("href found by regex: " + href);
			if(matcher.find()) {
				trackingNum = matcher.group(1);
			}
		}
		
		if(trackingNum == null) {
			throw new RuntimeException("Could not parse tracking number from page for order " + order.id);
		}

		final TrackingNumber carrierDetails = TrackingNumber.parse(trackingNum);
		driver.savePageSource();
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
