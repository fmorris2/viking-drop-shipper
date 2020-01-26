package test.org.vikingsoftware.dropshipper.order.tracking.history;

import org.junit.Test;

import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryUpdater;

public class TestTrackingHistoryUpdater {

	@Test
	public void test() {
		new TrackingHistoryUpdater().cycle();
	}
}
