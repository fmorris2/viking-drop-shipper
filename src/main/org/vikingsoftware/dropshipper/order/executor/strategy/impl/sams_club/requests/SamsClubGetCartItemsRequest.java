package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;

public class SamsClubGetCartItemsRequest extends SamsClubRequest {
	
	private static final String URL_PREFIX = "https://www.samsclub.com/cartservice/v1/carts/";
	private static final String URL_SUFFIX = "?response_groups=cart.medium";
	
	public SamsClubGetCartItemsRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public List<SamsClubCartItem> execute() {
		final String url = URL_PREFIX + getCookie("samsorder") + URL_SUFFIX;
		System.out.println("[SamsGetCartItemsRequest] About to dispatch GET request to URL: " + url);
		final HttpGet request = new HttpGet(url);
		addHeaders(request);
		
		final Optional<String> responseStr = super.sendRequest(client, request, HttpStatus.SC_OK);
		if(responseStr.isPresent()) {
			return convertResponseToCartItemList(responseStr.get());
		}
		
		return Collections.emptyList();
	}
	
	private void addHeaders(final HttpGet request) {
		request.addHeader("user-agent", DEFAULT_USER_AGENT);
		request.addHeader("content-type", "application/json");
		request.addHeader("accept", "application/json");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("dnt", "1");
		request.addHeader("wm_consumer.source_id", "2");
		request.addHeader("wm_tenant_id", "1");
		request.addHeader("wm_vertical_id", "3");
	}
	
	private List<SamsClubCartItem> convertResponseToCartItemList(final String response) {
		final List<SamsClubCartItem> cartItems = new ArrayList<>();
		try {
			final JSONObject json = new JSONObject(response);
			final JSONArray cartItemsArr = json.getJSONObject("payload")
					.getJSONObject("cart")
					.getJSONArray("cartItems");
			
			System.out.println("[SamsClubGetCartItemsRequest] Found " + cartItemsArr.length() + " cart items.");
			for(int i = 0; i < cartItemsArr.length(); i++) {
				final JSONObject cartItem = cartItemsArr.getJSONObject(i);
				final SamsClubCartItem cartItemPojo = convertCartItemJsonToPojo(cartItem);
				System.out.println("[SamsClubGetCartItemsRequest] Found Cart Item: " + cartItemPojo);
				cartItems.add(cartItemPojo);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return cartItems;
	}
	
	private SamsClubCartItem convertCartItemJsonToPojo(final JSONObject json) {
		final JSONObject itemInfo = json.getJSONObject("itemInfo");
		final JSONObject itemPriceInfo = json.getJSONObject("itemPriceInfo");
		
		final SamsClubItem item = new SamsClubItem(itemInfo.getString("itemNo"), itemInfo.getString("productId"),
				itemInfo.getString("skuId"));
		
		return new SamsClubCartItem.Builder()
				.cartItemId(json.getString("id"))
				.quantity(json.getInt("qty"))
				.itemCost(itemPriceInfo.getDouble("itemTotal"))
				.shippingCost(itemPriceInfo.getDouble("shippingAmount"))
				.item(item)
				.build();
	}
	
}
