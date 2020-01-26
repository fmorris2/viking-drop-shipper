package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl.sams;

import java.util.Optional;

import com.ebay.soap.eBLBaseComponents.ShipmentDeliveryStatusCodeType;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrderManager;
import main.org.vikingsoftware.dropshipper.core.data.tracking.TrackingEntry;
import main.org.vikingsoftware.dropshipper.core.tracking.ShippingCarrier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsOrderDetailsAPI;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsOrderState;
import main.org.vikingsoftware.dropshipper.order.tracking.error.UnknownTrackingIdException;
import main.org.vikingsoftware.dropshipper.order.tracking.handler.OrderTrackingHandler;

public class SamsClubOrderTrackingStrategy implements OrderTrackingHandler {
	
	@Override
	public Optional<TrackingEntry> getTrackingInfo(final ProcessedOrder order) {
		final SamsOrderDetailsAPI api = new SamsOrderDetailsAPI();
		TrackingEntry trackingEntry = null;
		if(api.parse(order.fulfillment_transaction_id)) {
			final Optional<String> orderState = api.getOrderState();
			if(orderState.isPresent()) {
				final SamsOrderState state = SamsOrderState.getStateForApiResponse(orderState.get());
				if(state == SamsOrderState.CANCELLED) {
					System.err.println("Identified cancelled Sam's Club order, marking processed order in DB...");
					ProcessedOrderManager.markAsCancelled(order.id);
				}
			}
			
			final Optional<String> trackingNum = api.getTrackingNumber();
			if(trackingNum.isPresent()) {
				trackingEntry = generateTrackingEntryFromTrackingNum(trackingNum.get());
			}
		}
		
		if(trackingEntry == null) {
			trackingEntry = parseTrackingEntryFromEmail(order);
		}
		
		return Optional.ofNullable(trackingEntry);
	}
	
	private TrackingEntry parseTrackingEntryFromEmail(final ProcessedOrder order) {
		System.out.println("Attempting to parse tracking entry from email...");
		final String trackingNum = SamsClubTrackingEmailRepository.get().getTrackingNumberForTransactionId(order.fulfillment_transaction_id);
		if(trackingNum != null) {
			return generateTrackingEntryFromTrackingNum(trackingNum);
		}
		
		System.out.println("Tracking number could not be found in email.");
		return null;
	}
	
	private TrackingEntry generateTrackingEntryFromTrackingNum(final String trackingNum) {
		final ShippingCarrier carrier = ShippingCarrier.getCarrierFromTrackingNum(trackingNum);
		if(carrier == null) {
			throw new UnknownTrackingIdException("Unknown courier for tracking number: " + trackingNum);
		}

		final String courierName = carrier.name().replaceAll("[^a-zA-Z0-9\\-\\s]", "").replace("_", " ").toLowerCase();
		System.out.println("Tracking #: " + trackingNum);
		System.out.println("Carrier: " + courierName);
		return new TrackingEntry(courierName, trackingNum, ShipmentDeliveryStatusCodeType.IN_TRANSIT);
	}
}
