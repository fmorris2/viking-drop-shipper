package main.org.vikingsoftware.dropshipper.core.tracking;

import java.util.HashMap;
import java.util.Map;

public enum TrackingStatus {
	
	UNKNOWN(0),
	DELIVERED(1),
	TRANSIT(2),
	FAILURE(3),
	RETURNED(4),
	TRACKING_NUMBER_PARSED(5);
	
	private static final Map<Integer, TrackingStatus> valueToStatusCache = new HashMap<>();
	
	static {
		for(final TrackingStatus status : values()) {
			valueToStatusCache.put(status.value, status);
		}
	}
	
	public final int value;
	TrackingStatus(final int value) {
		this.value = value;
	}
	
	public static TrackingStatus getStatusFromValue(final int value) {
		return valueToStatusCache.get(value);
	}
	
	
	
}
