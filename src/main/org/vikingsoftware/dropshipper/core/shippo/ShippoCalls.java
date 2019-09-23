package main.org.vikingsoftware.dropshipper.core.shippo;

import com.shippo.exception.APIConnectionException;
import com.shippo.exception.APIException;
import com.shippo.exception.AuthenticationException;
import com.shippo.exception.InvalidRequestException;
import com.shippo.model.Track;

import main.mysterytracking.TrackingNumber;

public final class ShippoCalls {

	private ShippoCalls() {
		//util classes shouldn't be instantiated
	}
	
	public static boolean registerTrackingWebhook(final String trackingNum) {
		final TrackingNumber carrierDetails = TrackingNumber.parse(trackingNum);
		if(!carrierDetails.isCourierRecognized()) {
			System.out.println("Unknown courier for tracking number: " + trackingNum);
			return false;
		}
		
		final ShippoCarrier shippoCarrier = ShippoCarrier.getCarrier(carrierDetails.getCourierParentName());
		if(shippoCarrier == null) {
			System.out.println("Could not map shippo carrier from mysterytracking courier: " + carrierDetails.getCourierParentName());
			return false;
		}
		
		return registerTrackingWebhook(shippoCarrier, trackingNum);
	}
	
	/*
	 * TODO - Implement specific handling for each exception type
	 */
	public static boolean registerTrackingWebhook(final ShippoCarrier carrier, final String trackingNum) {
		try {
			final Track track = Track.registerTrackingWebhook(carrier.apiToken, trackingNum, "", ShippoApiContextManager.getLiveKey());
			//TODO - Is any validation needed with the Track object?
			return true;
		} catch (AuthenticationException e) {
			e.printStackTrace();
		} catch (InvalidRequestException e) {
			e.printStackTrace();
		} catch (APIConnectionException e) {
			e.printStackTrace();
		} catch (APIException e) {
			e.printStackTrace();
		}
		return false;
	}
}
