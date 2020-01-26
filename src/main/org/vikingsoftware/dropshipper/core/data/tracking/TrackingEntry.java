package main.org.vikingsoftware.dropshipper.core.data.tracking;

import com.ebay.soap.eBLBaseComponents.ShipmentDeliveryStatusCodeType;

public class TrackingEntry {

	public final String shippingService;
	public final String trackingNumber;
	public final ShipmentDeliveryStatusCodeType shipmentStatus;

	public TrackingEntry(final String service, final String num, final ShipmentDeliveryStatusCodeType shipmentStatus) {
		this.shippingService = service;
		this.trackingNumber = num;
		this.shipmentStatus = shipmentStatus;
	}
}
