package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

public class FulfillmentMapping {
	public final int id;
	public final int marketplace_listing_id;
	public final int fulfillment_listing_id;
	public final String item_options_mappings;
	
	public FulfillmentMapping(final int id, final int marketplace_listing_id, final int fulfillment_listing_id,
			final String item_options_mappings) {
		this.id = id;
		this.marketplace_listing_id = marketplace_listing_id;
		this.fulfillment_listing_id = fulfillment_listing_id;
		this.item_options_mappings = item_options_mappings;
	}
}
