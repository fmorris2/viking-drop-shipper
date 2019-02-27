package main.org.vikingsoftware.dropshipper.core.data.sku;

public class SkuMapping {
	
	public final int id;
	public final int marketplace_listing_id;
	public final String item_sku;
	public final String ali_express_options;
	
	private SkuMapping(final Builder builder) {
		this.id = builder.id;
		this.marketplace_listing_id = builder.marketplace_listing_id;
		this.item_sku = builder.item_sku;
		this.ali_express_options = builder.ali_express_options;
	}
	
	public static class Builder {
		public int id;
		public int marketplace_listing_id;
		public String item_sku;
		public String ali_express_options;
		
		public Builder id(final int id) {
			this.id = id;
			return this;
		}
		
		public Builder marketplace_listing_id(final int id) {
			this.marketplace_listing_id = id;
			return this;
		}
		
		public Builder item_sku(final String sku) {
			this.item_sku = sku;
			return this;
		}
		
		public Builder ali_express_options(final String options) {
			this.ali_express_options = options;
			return this;
		}
		
		public SkuMapping build() {
			return new SkuMapping(this);
		}
	} 
	
}
