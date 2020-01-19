package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public class SamsClubAddToCartRequest extends SamsRequest {
	
	private static final String URL_PREFIX = "http://www.samsclub.com/api/node/cartservice/v1/carts/";
	private static final String URL_SUFFIX = "/cartitems?response_groups=cart.medium";
	
	private final int quantity;
	private final String productId;
	private final String skuId;
	private final String itemNumber;
	
	private SamsClubAddToCartRequest(final Builder builder) {
		super(builder.client, builder.cookies);
		this.quantity = builder.quantity;
		this.productId = builder.productId;
		this.skuId = builder.skuId;
		this.itemNumber = builder.itemNumber;
	}
	
	public boolean execute() {
		final String url = URL_PREFIX + getCookie("samsorder") + URL_SUFFIX;
		System.out.println("[SamsClubAddToCartRequest] Formulating POST request for url: " + url);
		final HttpPost request = new HttpPost(url);
		addHeaders(request);
		addPayload(request);
		return sendRequest(client, request);
	}
	
	private void addHeaders(final HttpPost request) {
		request.addHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36");
		request.addHeader("content-type", "application/json");
		request.addHeader("accept-encoding", "gzip, deflate, br");
	}
	
	private void addPayload(final HttpPost request) {
		final String json = constructPayloadJsonString();
		System.out.println("[SamsClubAddToCartRequest] Constructing Payload w/ JSON: " + json);
		final StringEntity entity = new StringEntity(json,ContentType.APPLICATION_JSON);
		request.setEntity(entity);
	}
	
	private String constructPayloadJsonString() {
		final JSONObject jsonObj = new JSONObject();
		final JSONObject payloadObj = new JSONObject();
		final JSONArray itemsArr = new JSONArray();
		final JSONObject itemObj = new JSONObject();
		final JSONObject offerIdObj = new JSONObject();
		
		offerIdObj.put("USItemId", this.productId);
		offerIdObj.put("USVariantId", this.skuId);
		offerIdObj.put("USSellerId", 0);
		offerIdObj.put("itemNumber", this.itemNumber);
		
		itemObj.put("quantity", this.quantity);
		itemObj.put("channel", "online");
		itemObj.put("offerId", offerIdObj);
		
		itemsArr.put(itemObj);
		payloadObj.put("items", itemsArr);
		jsonObj.put("payload", payloadObj);
		
		return jsonObj.toString();
	}
	
	private boolean sendRequest(final WrappedHttpClient client, final HttpPost request) {
		try {
			final HttpResponse response = client.execute(request, client.createContextFromCookies(cookies));
			System.out.println("[SamsClubAddToCartRequest] Response: " + EntityUtils.toString(response.getEntity()));
			return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
		} catch(final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return false;
	}
	
	public static class Builder {
		private CookieStore cookies;
		private int quantity;
		private String productId;
		private String skuId;
		private String itemNumber;
		private WrappedHttpClient client;
		
		public Builder() {
			
		}
		
		public Builder(final FulfillmentListing listing) {
			productId = listing.product_id;
			skuId = listing.sku_id;
			itemNumber = listing.item_id;
		}
		
		public Builder cookies(final CookieStore cookies) {
			this.cookies = cookies;
			return this;
		}
		
		public Builder quantity(final int qty) {
			this.quantity = qty;
			return this;
		}
		
		public Builder productId(final String id) {
			this.productId = id;
			return this;
		}
		
		public Builder skuId(final String id) {
			this.skuId = id;
			return this;
		}
		
		public Builder itemNumber(final String num) {
			this.itemNumber = num;
			return this;
		}
		
		public Builder client(final WrappedHttpClient client) {
			this.client = client;
			return this;
		}
		
		public SamsClubAddToCartRequest build() {
			return new SamsClubAddToCartRequest(this);
		}
	}
}
