package main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy.impl;

import java.util.Optional;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.net.http.HttpClientManager;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubSessionProvider;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubAddToCartRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubCreateContractRequest;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubAddress;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.utils.SamsClubCartUtils;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.ShippingEstimation;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy.ShippingEstimatorStrategy;

public class SamsClubShippingEstimatorStrategy implements ShippingEstimatorStrategy {
	
	private static final Logger log = Logger.getLogger(SamsClubShippingEstimatorStrategy.class);
	
	private static final SamsClubShippingEstimationTestCase[] testCases = {
		new SamsClubShippingEstimationTestCase("NY", "13211"), //Syracuse NY
		new SamsClubShippingEstimationTestCase("NE", "68869"), //Central Nebraska
		new SamsClubShippingEstimationTestCase("CA", "95230") //Central CA
	};
	
	private static final SamsClubAddress.Builder addressBuilder = new SamsClubAddress.Builder()
			.firstName("Ronald")
			.lastName("McDonald")
			.addressLineOne("30 Big Mac Circle")
			.city("Mickey-Dees")
			.countryCode("US");

	@Override
	public Optional<ShippingEstimation> generateEstimation(final FulfillmentListing fulfillmentListing) {
		log.info("Generating Sams Club shipping estimation for fulfillment listing w/ id " + fulfillmentListing.id);
		
		final WrappedHttpClient client = HttpClientManager.get().getClient();
		final FulfillmentAccount account = FulfillmentAccountManager.get().getAccountById(15);
		SamsClubSessionProvider.get().getSession(account, client);
		
		if(!SamsClubCartUtils.clearCart(client)) {
			log.info("Failed to clear cart before estimating shipping for fulfillment listing w/ id " + fulfillmentListing.id);
			return Optional.empty();
		}
		
		final Optional<SamsClubResponse> addToCartAction = addItemToCart(client, fulfillmentListing);
		if(!addToCartAction.isPresent()) {
			log.info("Failed to add fulfillment listing w/ id " + fulfillmentListing.id + " to cart.");
			return Optional.empty();
		}
		
		log.info("Add to cart action: " + addToCartAction.get());
		
		return computeCeilShippingFromTestCases(client, fulfillmentListing);
	}
	
	private Optional<SamsClubResponse> addItemToCart(final WrappedHttpClient client, final FulfillmentListing listing) {
		final SamsClubAddToCartRequest request = new SamsClubAddToCartRequest.Builder(listing)
				.client(client)
				.quantity(1)
				.build();
		
		return request.execute();
	}
	
	private Optional<ShippingEstimation> computeCeilShippingFromTestCases(final WrappedHttpClient client, 
			final FulfillmentListing listing) {
		
		double shippingCeil = 0D;
		
		for(final SamsClubShippingEstimationTestCase testCase : testCases) {
			final SamsClubAddress addr = addressBuilder
					.stateOrProvinceCode(testCase.state)
					.postalCode(testCase.postalCode)
					.build();
			
			final SamsClubCreateContractRequest createContractReq = new SamsClubCreateContractRequest(client, addr);
			final Optional<JSONObject> json = createContractReq.execute();
			if(json.isPresent()) {
				log.info("Contract for test case " + testCase + ": " + json.get());
				try {
					final double shipping = json.get()
							.getJSONObject("payload")
							.getJSONObject("purchaseContract")
							.getJSONObject("totals")
							.getDouble("totalOrderShippingAmount");
					
					shippingCeil = Math.max(shippingCeil, shipping);
				} catch(final JSONException e) {
					log.warn("Purchase contract JSON may have changed. Failed to parse prepay summary from JSON: " + json.get());
					return Optional.empty();
				}
			} else {
				log.warn("Failed to generate contract for test case " + testCase);
				return Optional.empty();
			}
		}
		
		log.info("Max shipping cost: " + shippingCeil);
		
		final ShippingEstimation estimation = new ShippingEstimation.Builder()
				.fulfillmentListing(listing)
				.estimatedCost(shippingCeil)
				.build();
		
		return Optional.of(estimation);
	}

}
