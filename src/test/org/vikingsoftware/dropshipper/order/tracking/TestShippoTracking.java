package test.org.vikingsoftware.dropshipper.order.tracking;

import org.junit.Before;
import org.junit.Test;

import com.shippo.Shippo;
import com.shippo.model.Track;

import main.org.vikingsoftware.dropshipper.core.shippo.ShippoCarrier;

public class TestShippoTracking {

	private static final String SHIPPO_LIVE_API_KEY = "shippo_live_f8bba555338ff892031378a0f43a7eb283f2cc95";
	private static final String SHIPPO_TEST_API_KEY = "shippo_test_7a9a05214c3fcc9fbf4fb65bbe36f3eec19ca2a2";
	@Before
	public void setAPIInfo() {
		
		Shippo.apiKey = SHIPPO_LIVE_API_KEY;
		Shippo.apiVersion = "2018-02-08";
	}
	
	@Test
	public void testTracking() {
		try {
			//Track.registerTrackingWebhook(ShippoCarrier.ONTRAC.apiToken, "C11732391285793", "", Shippo.apiKey);
			final Track track = Track.getTrackingInfo(ShippoCarrier.USPS.apiToken, "9200190230268800130152", Shippo.apiKey);
			System.out.println("Track: " + track);
			System.out.println(track.getTrackingStatus().getStatusDetails());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
