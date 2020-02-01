package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.VSDropShipper;
import main.org.vikingsoftware.dropshipper.core.data.customer.order.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.JsonAPIParser;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubLoginResponse;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.error.FatalOrderExecutionException;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;
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
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress.Builder;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubCartItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubItem;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubOrderPricingDetails;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubPlaceOrderRequestDependencies;

public class SamsClubOrderExecutionStrategy implements OrderExecutionStrategy {
	
	private static final Logger LOG = Logger.getLogger(SamsClubOrderExecutionStrategy.class);
	
	private static final int MAX_ADDRESS_LINE_LENGTH = 35;
	private static final int MAX_NAME_LINE_LENGTH = 25;
	
	private final Set<FulfillmentAccount> preparedAccounts = new HashSet<>();

	@Override
	public Optional<ProcessedOrder> order(final CustomerOrder order, final FulfillmentAccount account,
			final FulfillmentListing listing) {
		
		if(!preparedAccounts.contains(account)) {
			SamsClubSessionProvider.get().clearSession(account);
			preparedAccounts.add(account);
		}
		
		LOG.info("Initiating order process for customer order: " + order);
		
		final SamsClubItem itemToPurchase = new SamsClubItem(listing.item_id, listing.product_id, listing.sku_id);	
		final Optional<SamsClubAddress.Builder> address = convertCustomerOrderToSamsClubAddress(order);
		if(address.isPresent()) {
			
			SamsClubAddress builtAddress = null;
			SamsClubOrderPricingDetails pricing = null;
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
			} else if((pricing = verifyPurchaseContract(client, address.get(), order, listing, account, itemToPurchase)) == null) {
				LOG.warn("Failed to verify purchase contract.");
			} else if((placeOrderDependencies = generatePlaceOrderDependencies(pricing, client)) == null) {
				LOG.warn("Failed to generate place order dependencies.");
			} else if((placeOrderResponse = submitPlaceOrderRequest(client, placeOrderDependencies).orElse(null)) == null) {
				LOG.warn("Failed to submit place order request.");
			} else { //Convert order JSON response to processed order
				return Optional.of(convertJsonToProcessedOrder(order, listing, pricing, account, placeOrderResponse));
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
		address.phone(VSDropShipper.VS_PHONE_NUM);
		
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
				.quantity(order.snapshot_fulfillment_quantity_multiplier)
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
	
	private SamsClubOrderPricingDetails verifyPurchaseContract(final WrappedHttpClient client, final Builder address, 
			final CustomerOrder order, final FulfillmentListing listing, final FulfillmentAccount account,
			final SamsClubItem itemToPurchase) {
		final JSONObject contract = getCurrentContract(client);
		
		if(contract == null) {
			LOG.warn("Failed to get current purchase contract - Contract is null.");
			return null;
		}
		
		final JSONObject payload = contract.getJSONObject("payload");
		
		//verify account details
		if(!verifyAccountDetails(account, payload)) {
			LOG.warn("Failed to verify account details against purchase contract.");
			return null;
		}
		
		final JSONObject fulfillmentGroups = payload.getJSONObject("fulfillmentGroups");
		final JSONObject shippingJson = fulfillmentGroups.getJSONObject("shipping");
		final JSONObject addressJson = shippingJson.getJSONObject("shippingAddress");
		
		//verify shipping address
		if(!verifyShippingDetails(addressJson, address)) {
			LOG.warn("Failed to verify shipping address against purchase contract.");
			return null;
		}
		
		final JSONObject d2h = shippingJson.getJSONObject("d2h");
		final JSONArray packageGroups = d2h.getJSONArray("packageGroups");
		final JSONObject packageGroup = packageGroups.getJSONObject(0);
		final JSONArray itemsArrJson = packageGroup.getJSONArray("items");
		final JSONObject itemJson = itemsArrJson.getJSONObject(0);
		
		if(itemsArrJson.length() > 1) {
			LOG.warn("Contract contrains erroneous items.");
			return null;
		}
		
		//verify item details
		if(!verifyItemDetails(itemJson, order, itemToPurchase)) {
			LOG.warn("Failed to verify item details against purchase contract.");
			return null;
		}
		
		final JSONObject pricingJson = payload.getJSONObject("prepaySummary");
		
		//verify final pricing
		
		final SamsClubOrderPricingDetails pricing = verifyFinalPricing(pricingJson, order, listing, account);
		if(pricing == null) {
			LOG.warn("Failed to verify final pricing against purchase contract.");
			return null;
		}
		
		return pricing;
	}
	
	private boolean verifyAccountDetails(final FulfillmentAccount account, final JSONObject contract) {
		final String accountEmail = account.username;
		final String contractEmail = contract.getString("email");
		return Objects.equals(accountEmail, contractEmail);
	}
	
	private boolean verifyShippingDetails(final JSONObject json, final Builder address) {
		final SamsClubAddress contractAddress = addrFromJson(json);
		if(!Objects.equals(contractAddress, address.build())) {
			System.out.println("----------------------------- PROVIDED ADDRESS --------------------------");
			System.out.println(address.build());
			System.out.println("----------------------------- PROVIDED ADDRESS --------------------------");
			System.out.println("----------------------------- CONTRACT ADDRESS --------------------------");
			System.out.println(addrFromJson(json));
			System.out.println("----------------------------- CONTRACT ADDRESS --------------------------");
			return false;
		}
		
		return true;
	}

	private boolean verifyItemDetails(final JSONObject contract, final CustomerOrder order, 
			final SamsClubItem itemToPurchase) {
		
		final int contractQty = contract.getInt("quantity");
		if(contractQty != order.fulfillment_purchase_quantity) {
			LOG.warn("Contract Quantity (" + contractQty + ")"
					+ " is inconsistent with Customer Order Quantity (" + order.fulfillment_purchase_quantity + ")");
			return false;
		}
		
		final SamsClubItem contractItem = new SamsClubItem(contract.getString("itemNumber"), contract.getString("productId"),
				contract.getString("skuId"));
		
		if(!Objects.equals(contractItem, itemToPurchase)) {
			LOG.warn("Contract item is inconsistent with item to purchase.");
			System.out.println("----------------------------- PROVIDED ITEM TO PURCHASE --------------------------");
			System.out.println(itemToPurchase);
			System.out.println("----------------------------- PROVIDED ITEM TO PURCHASE --------------------------");
			System.out.println("----------------------------- CONTRACT ITEM TO PURCHASE --------------------------");
			System.out.println(contractItem);
			System.out.println("----------------------------- CONTRACT ITEM TO PURCHASE --------------------------");
			return false;
		}
		
		return true;
	}

	private SamsClubOrderPricingDetails verifyFinalPricing(final JSONObject contract, final CustomerOrder order, 
			final FulfillmentListing listing, final FulfillmentAccount account) {
		
		final SamsClubOrderPricingDetails pricing = new SamsClubOrderPricingDetails(contract);
		
//		if(pricing.shipping > 0) {
//			LOG.warn("Fulfillment Account has lost free shipping!");
//			FulfillmentAccountManager.get().markAccountAsDisabled(account);
//			return null;
//		}
		
		final Optional<Double> transactionSum = order.getTransactionSum();
		
		if(transactionSum.isEmpty()) {
			LOG.warn("Failed to sum transactions for customer order " + order.id);
			return null;
		}
		
		final double profit = transactionSum.get() - pricing.total;
		
		if(profit < 0 && !FulfillmentManager.isDisregardingProfit()) {
			LOG.warn("Detected fulfillment at loss for customer order " + order.id + ": -$" + profit + " profit.");
			return null;
		}
		
		pricing.profit = profit;
		
		return pricing;
	}
	
	private Optional<JSONObject> submitPlaceOrderRequest(final WrappedHttpClient client,
			final SamsClubPlaceOrderRequestDependencies placeOrderDependencies) {
		final SamsClubPlaceOrderRequest placeOrderReq = new SamsClubPlaceOrderRequest(client, placeOrderDependencies);
		return placeOrderReq.execute();
	}

	private SamsClubPlaceOrderRequestDependencies generatePlaceOrderDependencies(final SamsClubOrderPricingDetails pricing,
			final WrappedHttpClient client) {
		final Optional<JSONObject> paymentDetails = new SamsClubGetPaymentIdRequest(client).execute();
		
		if(!paymentDetails.isPresent()) {
			LOG.warn("Failed to get payment id.");
			return null;
		}
		
		final SamsClubPlaceOrderRequestDependencies placeOrderDependencies = 
				new SamsClubPlaceOrderRequestDependencies.Builder()
					.paymentId(paymentDetails.get()
							.getJSONArray("cards")
							.getJSONObject(0)
							.getString("pid"))
					.amount(pricing.total)
					.build();
		return placeOrderDependencies;
	} 
	
	private SamsClubAddress addrFromJson(final JSONObject json) {
		return new SamsClubAddress.Builder()
				.addressId(JsonAPIParser.getString(json, "addressId"))
				.addressType(JsonAPIParser.getString(json, "addressType"))
				.firstName(JsonAPIParser.getString(json, "firstName"))
				.lastName(JsonAPIParser.getString(json, "lastName"))
				.addressLineOne(JsonAPIParser.getString(json, "addressLineOne"))
				.addressLineTwo(JsonAPIParser.getString(json, "addressLineTwo"))
				.city(JsonAPIParser.getString(json, "city"))
				.stateOrProvinceCode(JsonAPIParser.getString(json, "state"))
				.postalCode(JsonAPIParser.getString(json, "postalCode"))
				.countryCode(JsonAPIParser.getString(json, "country"))
				.phone(JsonAPIParser.getString(json, "phone"))
				.phoneNumberType(JsonAPIParser.getString(json, "phoneType"))
				.dockDoorPresent(JsonAPIParser.getBoolean(json, "dockDoorPresent"))
				.build();		
	}
	
	private ProcessedOrder convertJsonToProcessedOrder(final CustomerOrder order,
			final FulfillmentListing listing, final SamsClubOrderPricingDetails pricing,
			final FulfillmentAccount account, final JSONObject json) {
		Optional<String> orderId = Optional.empty();
		try {
			orderId = Optional.of(json.getJSONObject("payload").getString("orderid"));
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		if(orderId.isEmpty()) {
			FulfillmentManager.disableOrderExecution(FulfillmentPlatforms.SAMS_CLUB.getId());
			throw new FatalOrderExecutionException("Failed to parse order id from Sams API!");
		}
		return new ProcessedOrder.Builder()
				.customer_order_id(order.id)
				.fulfillment_listing_id(listing.id)
				.fulfillment_account_id(account.id)
				.fulfillment_transaction_id(orderId.get())
				.buy_subtotal(pricing.subTotal)
				.buy_sales_tax(pricing.salesTax)
				.buy_shipping(pricing.shipping)
				.buy_product_fees(pricing.productFees)
				.buy_total(pricing.total)
				.profit(pricing.profit)
				.date_processed(System.currentTimeMillis())
				.build();
	}
}
