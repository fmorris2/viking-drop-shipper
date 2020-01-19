package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsPurchaseContractDependencies;

public class SamsClubGetCartItemsRequest extends SamsRequest {
	
	private static final String PURCHASE_CONTRACT_DEPENDENCIES_URL = "https://www.samsclub.com/sams/cart/cart.jsp";
	private static final String URL_PREFIX = "http://www.samsclub.com/cartservice/v1/carts/";
	private static final String URL_SUFFIX = "?response_groups=cart.medium";
	
	private SamsPurchaseContractDependencies purchaseContractDependencies;
	
	public SamsClubGetCartItemsRequest(final WrappedHttpClient client, final CookieStore cookieStore) {
		super(client, cookieStore);
		this.client = client;
	}
	
	public SamsPurchaseContractDependencies getPurchaseContractDependencies() {
		return purchaseContractDependencies;
	}
	
	public List<SamsClubCartItem> execute() {
		createPurchaseContractDependencies();
		final String url = URL_PREFIX + getCookie("samsorder") + URL_SUFFIX;
		System.out.println("[SamsGetCartItemsRequest] About to dispatch GET request to URL: " + url);
		final HttpGet request = new HttpGet(url);
		addHeaders(request);
		
		return sendRequest(client, request);
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
	
	private List<SamsClubCartItem> sendRequest(final WrappedHttpClient client, final HttpGet request) {
		try {
			final HttpResponse response = client.execute(request, client.createContextFromCookies(cookies));
			final String responseStr = EntityUtils.toString(response.getEntity());
			System.out.println("[SamsClubGetCartItemsRequest] Response: " + responseStr);
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return convertResponseToCartItemList(responseStr);
			}
		} catch(final IOException e) {
			e.printStackTrace();
			HttpClientManager.get().flag(client);
		}
		
		return Collections.emptyList();
	}
	
	private boolean createPurchaseContractDependencies() {
		try {
			final String html = Jsoup.connect(PURCHASE_CONTRACT_DEPENDENCIES_URL)
					.cookies(getCookieMap())
					.userAgent(DEFAULT_USER_AGENT)
					.ignoreContentType(true)
					.get()
					.data();
			
			purchaseContractDependencies = new SamsPurchaseContractDependencies(html);
		} catch(final IOException e) {
			e.printStackTrace();
		}
		
		return false;
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
