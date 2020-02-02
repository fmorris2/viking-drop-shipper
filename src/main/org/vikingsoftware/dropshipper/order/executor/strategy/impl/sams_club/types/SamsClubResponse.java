package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

public class SamsClubResponse {
	
	public final int statusCode;
	public final String response;
	
	public SamsClubResponse(final int statusCode, final String response) {
		this.statusCode = statusCode;
		this.response = response;
	}
	
	@Override
	public String toString() {
		return "["+statusCode+"] " + response;
	}
}
