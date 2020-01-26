package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.JsonAPIParser;

public final class SamsClubOrderDetailsAPI extends JsonAPIParser {
	
	private static final String API_BASE_URL = "http://www.samsclub.com/api/node/vivaldi/v1/orders/details?orderId=";
	private static final String API_URL_ARGS = "&responseGroup=FULL";
	
	private Optional<JSONObject> json;
	private Optional<JSONObject> details;
	private Optional<JSONObject> shipments;
	
	public Optional<String> getAPIStatus() {
		if(json.isPresent()) {
			return Optional.ofNullable(getString(json.get(), "status"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getOrderState() {
		if(details.isPresent()) {
			return Optional.ofNullable(getString(details.get(), "orderState"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getTrackingNumber() {
		if(shipments.isPresent()) {
			final JSONArray shipmentElements = shipments.get().names();
			if(shipmentElements != null && shipmentElements.length() > 0) {
				final JSONArray firstShipmentArr = getJsonArr(shipments.get(), shipmentElements.getString(0));
				if(firstShipmentArr != null && firstShipmentArr.length() > 0) {
					final JSONObject firstShipmentObj = firstShipmentArr.getJSONObject(0);
					return Optional.ofNullable(getString(firstShipmentObj, "trackingNo"));
				}
			}
		}
		
		return Optional.empty();
	}

	public boolean parse(final String orderId) {
		String apiUrl = null;
		HttpClientManager.get().rotateClient();
		final FulfillmentAccount acc = FulfillmentAccountManager.get().getAccountByTransactionId(orderId);
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		HttpResponse response = null;
		try {
			reset();
			apiUrl = API_BASE_URL + orderId + API_URL_ARGS;
			if(acc == null) {
				return false;
			}
			final SamsClubLoginResponse session = SamsClubSessionProvider.get().getSession(acc, client);
			if(session == null || session.cookies.isEmpty()) {
				return false;
			}
			final HttpGet get = new HttpGet(apiUrl);
			get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
			get.addHeader("Content-Type", "application/json");
			get.addHeader("Accept-Charset", "utf-8");
			client.setCookies("samsclub.com", "/", session.cookies);
			response = client.execute(get);
			
			final String rawJson = EntityUtils.toString(response.getEntity());
			
			json = Optional.of(new JSONObject(rawJson));
			json.ifPresent(obj -> {
				details = Optional.ofNullable(getJsonObj(obj, "details"));
				details.ifPresent(detailsObj -> {
					shipments = Optional.ofNullable(getJsonObj(detailsObj, "shipments"));
				});
			});
			System.out.println(json);
			return true;
		} catch(final IOException e) {
			System.out.println("Clearing Sams Club Session for account: " + acc);
			SamsClubSessionProvider.get().clearSession(acc);
			e.printStackTrace();
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			client.release(response);
		}
		
		return false;
	}
	
	private void reset() {
		json = Optional.empty();
		details = Optional.empty();
		shipments = Optional.empty();
	}
	
	
}
