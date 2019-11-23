package main.org.vikingsoftware.dropshipper.core.tracking;

import java.util.Arrays;
import java.util.List;

import main.mysterytracking.TrackingNumber;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.tracking.history.strategy.FedexTrackingHistoryParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.tracking.history.strategy.LasershipTrackingHistoryParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.tracking.history.strategy.OntracTrackingHistoryParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.tracking.history.strategy.UpsTrackingHistoryParsingStrategy;
import main.org.vikingsoftware.dropshipper.order.tracking.history.strategy.UspsTrackingHistoryParsingStrategy;

public enum ShippingCarrier {
	APC_POSTAL,
	AUSTRALIA_POST,
	ARAMEX,
	ASENDIA,
	AXLE_HIRE,
	BORDER_GURU,
	BOXBERRY,
	BRING,
	CANADA_POST,
	CDL,
	CORREIOS_BRAZIL,
	CORREOS_ESPANA,
	COLLECT_PLUS,
	COURIERS_PLEASE,
	DEUTSCHE_POST,
	DHL_BENELUX,
	DHL_GERMANY,
	DHL_ECOMMERCE,
	DHL_EXPRESS,
	DPD_GERMANY,
	DPD_UK,
	ESTAFETA,
	FASTWAY_AUSTRALIA,
	FEDEX(new FedexTrackingHistoryParsingStrategy()),
	GLS_GERMANY,
	GLD_FRANCE,
	GLOBEGISTICS,
	GOPHR,
	GSO,
	HERMES_UK,
	HONG_KONG_POST,
	LASERSHIP(new LasershipTrackingHistoryParsingStrategy()),
	MONDIAL_RELAY,
	NEW_ZEALAND_POST,
	NEWGISTICS,
	NIPPON_EXPRESS,
	ONTRAC(new OntracTrackingHistoryParsingStrategy()),
	ORANGE_DS,
	PARCEL,
	POSTI,
	PUROLATOR,
	RR_DONNELLEY,
	RUSSIAN_POST,
	SENDLE,
	SKY_POSTAL,
	STUART,
	UPS(new UpsTrackingHistoryParsingStrategy()),
	USPS(new UspsTrackingHistoryParsingStrategy()),
	YODEL;
	
	private static final List<Pair<String, ShippingCarrier>> REGEX_PATTERNS = Arrays.asList(
	   new Pair<>("1LS\\d{12}", LASERSHIP),
	   new Pair<>("\\d{22}", USPS)
	);
	
	public final TrackingHistoryParsingStrategy trackingHistoryParsingStrategy;
	
	ShippingCarrier() {
		trackingHistoryParsingStrategy = null;
	}
	
	ShippingCarrier(final TrackingHistoryParsingStrategy strategy) {
		this.trackingHistoryParsingStrategy = strategy;
	}
	
	/*
	 * Used for mapping MysteryTracking identifiers to the appropriate ShippoCarrier enum entry
	 */
	public static ShippingCarrier getCarrierFromIdentifier(final String identifier) {
		switch(identifier.toLowerCase()) {
		
			case "dhl":
				return DHL_EXPRESS;
			case "fedex":
				return FEDEX;
			case "ontrac":
				return ONTRAC;
			case "ups":
				return UPS;
			case "usps":
			case "united states postal service":
				return USPS;
					
		}
		
		System.err.println("ShippingCarrier#getCarrierFromIdentifier - Could not map identifier " + identifier + " to ShippingCarrier");
		return null;
	}
	
	public static ShippingCarrier getCarrierFromTrackingNum(final String trackingNumber) {
		ShippingCarrier carrier = null;
		
		if(trackingNumber != null) {
			final TrackingNumber mysteryTrackingApiRes = TrackingNumber.parse(trackingNumber);
			
			if(mysteryTrackingApiRes != null && mysteryTrackingApiRes.isCourierRecognized()) {
				carrier = getCarrierFromIdentifier(mysteryTrackingApiRes.getCourierParentName());
			}
			
			if(carrier == null) {
				carrier = determineCarrier(trackingNumber);
			}
		}
		
		return carrier;
	}
	
	private static ShippingCarrier determineCarrier(final String trackingNumber) {
		System.out.println("determineCarrier("+trackingNumber+")");
		if(trackingNumber != null) {
			for(final Pair<String, ShippingCarrier> regex : REGEX_PATTERNS) {
				if(trackingNumber.matches(regex.left)) {
					return regex.right;
				}
			}
		}
		
		return null;
	}
}
