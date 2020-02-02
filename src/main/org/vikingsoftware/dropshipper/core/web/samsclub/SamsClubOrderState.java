package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.HashMap;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public enum SamsClubOrderState {
	RECENTLY_PLACED("recently placed order"),
	ORDER_RECEIVED("order received"),
	IN_PROGRESS("in progress"),
	PACKAGE_DATA_TRANSMITTED("package data transmitted to fedex"),
	ARRIVED_AT_FEDEX_LOCATION("arrived at fedex location"),
	DEPARTED_FEDEX_LOCATION("departed fedex location"),
	SHIPPED("shipped"),
	IN_TRANSIT("in transit"),
	OUT_FOR_DELIVERY("out for delivery"),
	DELIVERED("delivered"),
	CANCELLED("canceled");
	
	private static final Map<String, SamsClubOrderState> stateMappings = new HashMap<>();
	
	static {
		for(final SamsClubOrderState state : values()) {
			stateMappings.put(state.apiResponse, state);
		}
	}
	
	private final String apiResponse;
	SamsClubOrderState(final String apiResponse) {
		this.apiResponse = apiResponse;
	}
	
	public static SamsClubOrderState getStateForApiResponse(final String apiResponse) {
		final SamsClubOrderState state = stateMappings.get(apiResponse.toLowerCase());
		
		if(state == null) {
			System.err.println("Unknown Sam's Club Order state: " + apiResponse);
			DBLogging.high(SamsClubOrderState.class, "Unknown Sam's Club order state for api response " + apiResponse, new RuntimeException());
		}
		
		return state;
	}
}