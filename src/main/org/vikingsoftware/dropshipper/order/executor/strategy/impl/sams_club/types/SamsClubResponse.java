package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

public class SamsClubResponse {
	
	public final int statusCode;
	public final String response;
	public final boolean success;
	
	public SamsClubResponse(final int statusCode, final String response, final boolean success) {
		this.statusCode = statusCode;
		this.response = response;
		this.success = success;
	}
	
	@Override
	public String toString() {
		return "["+statusCode+"] " + response;
	}
}
