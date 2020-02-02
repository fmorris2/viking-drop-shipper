package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.JsonAPIParser;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;

public class SamsClubListAddressesRequest extends SamsClubRequest {
	
	private static final String URL = "https://www.samsclub.com/api/node/vivaldi/v1/account/shipping/addresses";

	public SamsClubListAddressesRequest(WrappedHttpClient client) {
		super(client);
	}

	public List<SamsClubAddress> execute() {
		final List<SamsClubAddress> addresses = new ArrayList<>();
		try {
			final HttpGet request = new HttpGet(URL);
			System.out.println("[SamsClubListAddressesRequest] About to dispatch GET request to " + URL);
			addHeaders(request);
			final SamsClubResponse response = this.sendRequest(client, request, HttpStatus.SC_OK).orElse(null);
			if(response != null) {
				System.out.println("[SamsClubListAddressesRequest] Response String: " + response);
				addresses.addAll(convertJsonToAddressPojos(new JSONObject(response)));
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
			
		return addresses;
	}
	
	private void addHeaders(final HttpRequestBase request) {
		request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.5");
		request.addHeader("cache-control", "max-age=0");
		request.addHeader("dnt", "1");
		request.addHeader("sec-fetch-mode", "navigate");
		request.addHeader("sec-fetch-site", "none");
		request.addHeader("sec-fetch-user", "?1");
		request.addHeader("upgrade-insecure-requests", "1");
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
	}
	
	private List<SamsClubAddress> convertJsonToAddressPojos(final JSONObject json) {
		final List<SamsClubAddress> addresses = new ArrayList<>();
		
		final JSONArray addressesArr = json.getJSONArray("addresses");
		for(int i = 0; i < addressesArr.length(); i++) {
			final JSONObject addressObj = addressesArr.getJSONObject(i);
			addresses.add(addrFromJson(addressObj));
		}
		
		return addresses;
	}
	
	private SamsClubAddress addrFromJson(final JSONObject json) {
		final JSONObject addr = json.getJSONObject("address");
		return new SamsClubAddress.Builder()
				.addressId(JsonAPIParser.getString(json, "addressId"))
				.addressType(JsonAPIParser.getString(json, "addressType"))
				.firstName(JsonAPIParser.getString(json, "firstName"))
				.lastName(JsonAPIParser.getString(json, "lastName"))
				.addressLineOne(JsonAPIParser.getString(addr, "addressLineOne"))
				.addressLineTwo(JsonAPIParser.getString(addr, "addressLineTwo"))
				.city(JsonAPIParser.getString(addr, "city"))
				.stateOrProvinceCode(JsonAPIParser.getString(addr, "state"))
				.postalCode(JsonAPIParser.getString(addr, "postalCode"))
				.countryCode(JsonAPIParser.getString(addr, "countryCode"))
				.phone(JsonAPIParser.getString(json, "phone"))
				.phoneNumberType(JsonAPIParser.getString(json, "phoneNumberType"))
				.isDefault(JsonAPIParser.getBoolean(json, "isDefault"))
				.dockDoorPresent(JsonAPIParser.getBoolean(json, "dockDoorPresent"))
				.build();		
	}
}
