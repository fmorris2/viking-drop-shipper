package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubLoginResponse;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCartItemsRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubGetCurrentContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRefreshSamsOrderRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubRemoveFromCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubUpdateCartItemQuantityRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress.Builder;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubPlaceOrderRequestDependencies;

public class SamsClubOrderExecutionStrategy implements OrderExecutionStrategy {
	
	private static final Logger LOG = Logger.getLogger(SamsClubOrderExecutionStrategy.class);
	
	private static final int MAX_ADDRESS_LINE_LENGTH = 35;
	private static final int MAX_NAME_LINE_LENGTH = 25;

	@Override
	public Optional<ProcessedOrder> order(final CustomerOrder order, FulfillmentAccount account,
			final FulfillmentListing listing) {
		
		account = FulfillmentAccountManager.get().getAccountById(15);
		
		LOG.info("Initiating order process for customer order: " + order);
		
		final SamsClubItem itemToPurchase = new SamsClubItem(listing.item_id, listing.product_id, listing.sku_id);	
		final Optional<SamsClubAddress.Builder> address = convertCustomerOrderToSamsClubAddress(order);
		if(address.isPresent()) {
			
			SamsClubAddress builtAddress = null;
			SamsClubPlaceOrderRequestDependencies placeOrderDependencies = null;
			JSONObject placeOrderResponse = null;
			
			LOG.info("ItemToPurchase:\n" + itemToPurchase);
			LOG.info("Converted Customer Address:\n" + address.get().build());
			final WrappedHttpClient client = HttpClientManager.get().getClient();
			final SamsClubLoginResponse loginResponse = SamsClubSessionProvider.get().getSession(account, client);
			//GO THROUGH EACH REQUEST AND DON'T CONTINUE IF ONE FAILS IN THE CHAIN
			if(loginResponse == null) {
				LOG.warn("Failed to successfully log in for fulfillment account: " + account);
			} else if(!refreshSamsOrder(client)) {
				LOG.warn("Failed to refresh the order successfully.");
			} else if(!addItemToCart(order, itemToPurchase, client)) {
				LOG.warn("Failed to add item to cart: " + itemToPurchase);
			} else if(!verifyCart(order, itemToPurchase, client)) {
				LOG.warn("Failed to verify the cart.");
			} else if((builtAddress = generateAddress(client, address.get())) == null) {
				LOG.warn("Failed to generate address.");
			} else if(!createPurchaseContract(client, builtAddress)) {
				LOG.warn("Failed to create purchase contract.");
			} else if(!verifyPurchaseContract(client, address.get(), order, listing)) {
				LOG.warn("Failed to verify purchase contract.");
			} else if((placeOrderDependencies = generatePlaceOrderDependencies(client)) == null) {
				LOG.warn("Failed to generate place order dependencies.");
			} else if((placeOrderResponse = submitPlaceOrderRequest(client, placeOrderDependencies).orElse(null)) == null) {
				LOG.warn("Failed to submit place order request.");
			}
		}
		
		return Optional.empty();
	}
	
	private Optional<SamsClubAddress.Builder> convertCustomerOrderToSamsClubAddress(final CustomerOrder order) {
		final SamsClubAddress.Builder address = new SamsClubAddress.Builder();
		
		address.firstName(enforceLineLength(order.getFirstName(true), MAX_NAME_LINE_LENGTH));
		address.lastName(enforceLineLength(order.getLastName(true), MAX_NAME_LINE_LENGTH));	
		address.addressLineOne(enforceLineLength(order.buyer_street_address, MAX_ADDRESS_LINE_LENGTH));
		address.city(order.buyer_city);
		address.stateOrProvinceCode(order.getStateCode());
		address.postalCode(order.getFiveDigitZip());
		address.countryCode("US");
		
		if(order.buyer_apt_suite_unit_etc != null) {
			address.addressLineTwo(enforceLineLength(order.buyer_apt_suite_unit_etc, MAX_ADDRESS_LINE_LENGTH));
		}
		
		return Optional.of(address);
	}
	
	private String enforceLineLength(final String addr, final int maxLength) {
		return addr.substring(0, Math.min(addr.length(), maxLength));
	}
	
	private boolean refreshSamsOrder(final WrappedHttpClient client) {
		final SamsClubRefreshSamsOrderRequest refreshRequest = new SamsClubRefreshSamsOrderRequest(client);
		final Optional<JSONObject> response = refreshRequest.execute();
		if(response.isPresent()) {
			final Map<String, String> newSamsOrderCookie = new HashMap<>();
			newSamsOrderCookie.put("samsorder", response.get()
					.getJSONObject("payload")
					.getJSONObject("cart")
					.getString("id"));
			
			client.setCookies("samsclub.com", "/", newSamsOrderCookie);
			return true;
		}
		
		return false;
	}
	
	private boolean addItemToCart(final CustomerOrder order, final SamsClubItem item, final WrappedHttpClient client) {
		final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder()
				.productId(item.productId)
				.skuId(item.skuId)
				.itemNumber(item.itemNumber)
				.client(client)
				.quantity(order.fulfillment_purchase_quantity)
				.build();
		LOG.info("Attempting to submit add to cart request");
		return request.execute();
	}
	
	private boolean verifyCart(final CustomerOrder order, final SamsClubItem item, 
			final WrappedHttpClient client) {
		
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(client);
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		if(currentCartItems.isEmpty()) {
			LOG.warn("Current cart is empty!");
			return false;
		}
		
		if(!adjustCartAsNecessary(currentCartItems, item, client, order)) {
			LOG.warn("Failed to adjust cart!");
			return false;
		}
		
		if(!verifyCart(client, item, order)) {
			LOG.warn("Failed to do final cart verification!");
			return false;
		}
		
		return true;
	}
	
	private boolean adjustCartAsNecessary(final List<SamsClubCartItem> currentCartItems, final SamsClubItem itemToPurchase,
			final WrappedHttpClient client, final CustomerOrder order) {
		for (final SamsClubCartItem cartItem : currentCartItems) {
			if (cartItem.item.equals(itemToPurchase)) {
				if(cartItem.quantity != order.fulfillment_purchase_quantity) {
					final SamsClubCartItem updatedCartItem = new SamsClubCartItem.Builder().cartItemId(cartItem.cartItemId)
							.quantity(order.fulfillment_purchase_quantity).build();
	
					final SamsClubUpdateCartItemQuantityRequest updateQtyReq = new SamsClubUpdateCartItemQuantityRequest(
							client);
					if(!updateQtyReq.execute(updatedCartItem)) {
						return false;
					}
				}
			} else {
				final SamsClubRemoveFromCartRequest removeFromCartReq = new SamsClubRemoveFromCartRequest(client);
				if(!removeFromCartReq.execute(cartItem)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean verifyCart(final WrappedHttpClient client, final SamsClubItem itemToPurchase,
			final CustomerOrder order) {
		final SamsClubGetCartItemsRequest getCartReq = new SamsClubGetCartItemsRequest(client);
		final List<SamsClubCartItem> currentCartItems = getCartReq.execute();
		
		if(currentCartItems.isEmpty()) {
			return false;
		}
		
		if(currentCartItems.stream()
				.anyMatch(itm -> !itm.item.equals(itemToPurchase) || itm.quantity != order.fulfillment_purchase_quantity)) {
			return false;
		}
		
		return true;
	}
	
	private SamsClubAddress generateAddress(final WrappedHttpClient client, final SamsClubAddress.Builder address) {
		final Optional<SamsClubAddress> defaultAddr = SamsClubAddress.findDefaultAddress(client);
		if(defaultAddr.isEmpty()) {
			return null;
		}
		
		final SamsClubAddress builtAddress = address
				.addressId(defaultAddr.get().addressId)
				.build();
		
		return builtAddress;
	}
	
	private boolean createPurchaseContract(final WrappedHttpClient client, final SamsClubAddress address) {		
		final SamsClubCreateContractRequest createContractRequest = new SamsClubCreateContractRequest(client, address);
		return createContractRequest.execute().isPresent();
	}
	
	private JSONObject getCurrentContract(final WrappedHttpClient client) {
		final SamsClubGetCurrentContractRequest request = new SamsClubGetCurrentContractRequest(client);
		final Optional<JSONObject> response = request.execute();		
		return response.orElse(null);
	}
	
	private boolean verifyPurchaseContract(final WrappedHttpClient client, final Builder builder, 
			final CustomerOrder order, final FulfillmentListing listing) {
		final JSONObject contract = getCurrentContract(client);
		
		if(contract == null) {
			return false;
		}
		
		//verify account details
		
		//verify shipping address
		
		//verify item details
		
		//verify final pricing
		
		return true;
	}
	
	private Optional<JSONObject> submitPlaceOrderRequest(WrappedHttpClient client,
			SamsClubPlaceOrderRequestDependencies placeOrderDependencies) {
		return Optional.empty();
	}

	private SamsClubPlaceOrderRequestDependencies generatePlaceOrderDependencies(WrappedHttpClient client) {
		return null;
	}
}
