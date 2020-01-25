package test.org.vikingsoftware.dropshipper.order.executor.impl.sams_club;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCurrentContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRemoveFromCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubUpdateCartItemQuantityRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;

public class TestSamsClubPlaceOrderRequest extends SamsClubRequestTest {

	@Test
	public void testOrderSingleItem() {
		System.out.println("Adding single item to cart...");
		final SamsClubItem itemToPurchase = new SamsClubItem("980094558", "183038", "185741");
		final SamsClubAddToCartRequest addToCartReq = createAddToCartRequests(itemToPurchase).get(0);
		final WrappedHttpClient client = addToCartReq.getClient();
		
		System.out.println("Testing get cart...");
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(client);
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		Assert.assertTrue(!currentCartItems.isEmpty());
		
		Assert.assertTrue(adjustCartAsNecessary(currentCartItems, itemToPurchase, client));
		
		Assert.assertTrue(verifyCart(client, itemToPurchase));
		
		final SamsClubAddress addr = generateAddress(client);
		Assert.assertNotNull(addr);
		
		Assert.assertTrue(createPurchaseContract(client, addr));
		
		final JSONObject currentContract = getCurrentContract(client);
		Assert.assertNotNull(currentContract);

		Assert.assertTrue(verifyPurchaseContract(currentContract, addr));
		
		//final SamsClubPlaceOrderRequest placeOrderReq = new SamsClubPlaceOrderRequest(client);
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
	
	private SamsClubAddress generateAddress(final WrappedHttpClient client) {
		final Optional<SamsClubAddress> defaultAddr = SamsClubAddress.findDefaultAddress(client);
		if(defaultAddr.isEmpty()) {
			return null;
		}
		
		System.out.println("Default Address ID: " + defaultAddr.get().addressId);
		
		//TODO ENSURE YOU'RE PASSING ALL THE NECESSARY FIELDS HERE.
		final SamsClubAddress address = new SamsClubAddress.Builder()
				.addressId(defaultAddr.get().addressId)
				.firstName("Fred")
				.middleName("C")
				.lastName("Morrison Jr")
				.addressLineOne("310 Boston Rd")
				.city("Mattydale")
				.stateOrProvinceCode("NY")
				.postalCode("13211")
				.countryCode("US")
				.build();
		
		return address;
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
