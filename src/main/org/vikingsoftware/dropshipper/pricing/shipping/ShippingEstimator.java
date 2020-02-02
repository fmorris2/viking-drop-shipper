package main.org.vikingsoftware.dropshipper.pricing.shipping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.ShippingEstimation;
import main.org.vikingsoftware.dropshipper.pricing.shipping.estimator.strategy.ShippingEstimatorStrategyManager;

public class ShippingEstimator implements CycleParticipant {
	
	private static final String UNESTIMATED_LISTINGS_QUERY = "SELECT * FROM fulfillment_listing WHERE estimated_shipping_cost IS NULL";
	
	private static final Logger log = Logger.getLogger(ShippingEstimator.class);
	
	private final ShippingEstimatorStrategyManager strategyManager = new ShippingEstimatorStrategyManager();
	
	public static void main(final String[] args) {
		new ShippingEstimator().cycle();
	}
	
	@Override
	public void cycle() {
		log.info("Beginning shipping estimator cycle");
		final List<FulfillmentListing> unestimatedListings = generateUnestimatedListings();
		for(final FulfillmentListing listing : unestimatedListings) {
			log.info("Estimated shipping cost for fulfillment listing w/ id " + listing.id);
			final Optional<ShippingEstimation> estimation = strategyManager.generateShippingEstimation(listing);
			if(estimation.isPresent()) {
				log.info("Successfully estimated shipping cost for fulfillment listing w/ id " + listing.id + ": " + estimation.get());
			} else {
				log.warn("Failed to estimate shipping cost for fulfillment listing w/ id"  + listing.id);
			}
		}
	}
	
	private List<FulfillmentListing> generateUnestimatedListings() {
		final List<FulfillmentListing> listings = new ArrayList<>();
		
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery(UNESTIMATED_LISTINGS_QUERY)) {
			
			while(res.next()) {
				final FulfillmentPlatforms platform = FulfillmentPlatforms.getById(res.getInt("fulfillment_platform_id"));
				FulfillmentManager.generateFulfillmentListingFromResultSet(platform, res).ifPresent(listings::add);
			}
			
		} catch(final SQLException e) {
			log.warn("Failed to query DB for unestimated listings", e);
		}
		
		log.info("Generated " + listings.size() + " unestimated listings");
		return listings;
	}

}
