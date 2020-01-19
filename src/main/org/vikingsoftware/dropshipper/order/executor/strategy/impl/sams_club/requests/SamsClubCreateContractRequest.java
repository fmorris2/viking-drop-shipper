package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import org.apache.http.client.CookieStore;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubCreateContractRequest extends SamsRequest {
	
	private static final String URL_PREFIX = "http://www.samsclub.com/vapi/v1/create-contract/";
	
	public SamsClubCreateContractRequest(WrappedHttpClient client, CookieStore cookies) {
		super(client, cookies);
	}

}
