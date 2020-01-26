package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCurrentContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetPaymentIdRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubPlaceOrderRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRefreshSamsOrderRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRemoveFromCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubUpdateCartItemQuantityRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubPlaceOrderRequestDependencies;

public class TestSamsClubPlaceOrderRequest extends SamsClubRequestTest {

	@Test
	public void testOrderSingleItem() {
		final SamsClubItem itemToPurchase = new SamsClubItem("980207779", "162993", "123591");
		final SamsClubAddress.Builder addr = new SamsClubAddress.Builder()
				.firstName("Heidi")
				.lastName("Sholtis")
				.addressLineOne("105 South D Street")
				.city("Easley")
				.stateOrProvinceCode("SC")
				.postalCode("29640")
				.countryCode("US");
		Assert.assertTrue(orderItem(itemToPurchase, addr));
	}
	
	@Test
	public void testOrderMultipleItems() {
		final SamsClubItem itemToPurchase1 = new SamsClubItem("980207779", "162993", "123591");
		final SamsClubAddress.Builder addr1 = new SamsClubAddress.Builder()
				.firstName("Heidi")
				.lastName("Sholtis")
				.addressLineOne("105 South D Street")
				.city("Easley")
				.stateOrProvinceCode("SC")
				.postalCode("29640")
				.countryCode("US");
		
		final SamsClubItem itemToPurchase2 = new SamsClubItem("980094559", "prod22121370", "sku22675462");
		final SamsClubAddress.Builder addr2 = new SamsClubAddress.Builder()
				.firstName("Bren")
				.lastName("Rosa")
				.addressLineOne("105 S. D St")
				.city("Easley")
				.stateOrProvinceCode("SC")
				.postalCode("29640")
				.countryCode("US");
		
		Assert.assertTrue(orderItem(itemToPurchase1, addr1));
		Assert.assertTrue(orderItem(itemToPurchase2, addr2));
	}
	
	private boolean orderItem(final SamsClubItem item, final SamsClubAddress.Builder address) {
		System.out.println("Adding single item to cart...");
		final SamsClubAddToCartRequest addToCartReq = createAddToCartRequests(item).get(0);
		final WrappedHttpClient client = addToCartReq.getClient();

		System.out.println("Current sams order: " + addToCartReq.getCookie("samsorder"));
		
		final Optional<JSONObject> refreshSamsOrderResponse = new SamsClubRefreshSamsOrderRequest(client).execute();
		Assert.assertTrue(refreshSamsOrderResponse.isPresent());
		
		final Map<String, String> newSamsOrderCookie = new HashMap<>();
		newSamsOrderCookie.put("samsorder", refreshSamsOrderResponse.get()
				.getJSONObject("payload")
				.getJSONObject("cart")
				.getString("id"));
		
		System.out.println("New SamsOrder: " + newSamsOrderCookie.get("samsorder"));
		client.setCookies("samsclub.com", "/", newSamsOrderCookie);
		
		Assert.assertTrue(addToCartReq.execute());
		
		System.out.println("Testing get cart...");
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(client);
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		Assert.assertTrue(!currentCartItems.isEmpty());
		
		Assert.assertTrue(adjustCartAsNecessary(currentCartItems, item, client));
		
		Assert.assertTrue(verifyCart(client, item));
		
		System.out.println("Client cookies: " + client.getCookieStore().getCookies());
		
		final SamsClubAddress addr = generateAddress(client, address);
		Assert.assertNotNull(addr);
		
		Assert.assertTrue(createPurchaseContract(client, addr));
		
		final JSONObject currentContract = getCurrentContract(client);
		Assert.assertNotNull(currentContract);

		Assert.assertTrue(verifyPurchaseContract(currentContract, addr));
		
		final Optional<JSONObject> paymentDetails = new SamsClubGetPaymentIdRequest(client).execute();
		Assert.assertTrue(paymentDetails.isPresent());
		
		final SamsClubPlaceOrderRequestDependencies placeOrderDependencies = 
				new SamsClubPlaceOrderRequestDependencies.Builder()
					.paymentId(paymentDetails.get()
							.getJSONArray("cards")
							.getJSONObject(0)
							.getString("pid"))
					.amount(currentContract
							.getJSONObject("payload")
							.getJSONObject("prepaySummary")
							.getDouble("total"))
					.build();
		
		System.out.println("PlaceOrderDependencies: " + placeOrderDependencies);
		
		final SamsClubPlaceOrderRequest placeOrderReq = new SamsClubPlaceOrderRequest(client, placeOrderDependencies);
		final Optional<JSONObject> placeOrderResponse = placeOrderReq.execute();
		Assert.assertTrue(placeOrderResponse.isPresent());
		
		System.out.println("PLACE ORDER RESPONSE: " + placeOrderResponse.get());
		System.out.println("Client cookies: " + client.getCookieStore().getCookies());
		return true;
	}
	
	private boolean adjustCartAsNecessary(final List<SamsClubCartItem> currentCartItems, final SamsClubItem itemToPurchase,
			final WrappedHttpClient client) {
		for (final SamsClubCartItem cartItem : currentCartItems) {
			if (cartItem.item.equals(itemToPurchase)) {
				if(cartItem.quantity != 1) {
					System.out.println("Updating item to purchase to correct quantity...");
					final SamsClubCartItem updatedCartItem = new SamsClubCartItem.Builder().cartItemId(cartItem.cartItemId)
							.quantity(1).build();
	
					final SamsClubUpdateCartItemQuantityRequest updateQtyReq = new SamsClubUpdateCartItemQuantityRequest(
							client);
					if(!updateQtyReq.execute(updatedCartItem)) {
						return false;
					}
				}
			} else {
				System.out.println("Removing erroneous item from cart: " + cartItem);
				final SamsClubRemoveFromCartRequest removeFromCartReq = new SamsClubRemoveFromCartRequest(client);
				if(!removeFromCartReq.execute(cartItem)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean verifyCart(final WrappedHttpClient client, final SamsClubItem itemToPurchase) {
		System.out.println("Verifying cart...");
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(client);
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		if(currentCartItems.isEmpty()) {
			return false;
		}
		
		if(currentCartItems.stream()
				.anyMatch(itm -> !itm.item.equals(itemToPurchase))) {
			return false;
		}
		
		return true;
	}
	
	private SamsClubAddress generateAddress(final WrappedHttpClient client, final SamsClubAddress.Builder address) {
		final Optional<SamsClubAddress> defaultAddr = SamsClubAddress.findDefaultAddress(client);
		if(defaultAddr.isEmpty()) {
			return null;
		}
		
		System.out.println("Default Address ID: " + defaultAddr.get().addressId);
		
		final SamsClubAddress builtAddress = address
				.addressId(defaultAddr.get().addressId)
				.build();
		
		return builtAddress;
	}
	
	private JSONObject getCurrentContract(final WrappedHttpClient client) {
		final SamsClubGetCurrentContractRequest request = new SamsClubGetCurrentContractRequest(client);
		final Optional<JSONObject> response = request.execute();		
		System.out.println("Current contract: " + response.orElse(null));
		return response.orElse(null);
	}
	
	private boolean createPurchaseContract(final WrappedHttpClient client, final SamsClubAddress address) {		
		final SamsClubCreateContractRequest createContractRequest = new SamsClubCreateContractRequest(client, address);
		return createContractRequest.execute().isPresent();
	}
	
	private boolean verifyPurchaseContract(final JSONObject currentContract, final SamsClubAddress address) {
		
		return true;
	}
}
