package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

public class FulfillmentPlatform {
	public final int id;
	public final String platform_name;
	public final String platform_url;
	public final boolean can_execute_orders;
	public final boolean can_update_inventory;
	
	private FulfillmentPlatform(final Builder builder) {
		this.id = builder.id;
		this.platform_name = builder.platform_name;
		this.platform_url = builder.platform_url;
		this.can_execute_orders = builder.can_execute_orders;
		this.can_update_inventory = builder.can_update_inventory;
	}
	
	public static class Builder {
		private int id;
		private String platform_name;
		private String platform_url;
		private boolean can_execute_orders;
		private boolean can_update_inventory;
		
		public Builder id(final int id) {
			this.id = id;
			return this;
		}
		
		public Builder platform_name(final String name) {
			this.platform_name = name;
			return this;
		}
		
		public Builder platform_url(final String url) {
			this.platform_url = url;
			return this;
		}
		
		public Builder can_execute_orders(final boolean canExecute) {
			this.can_execute_orders = canExecute;
			return this;
		}
		
		public Builder can_update_inventory(final boolean canUpdate) {
			this.can_update_inventory = canUpdate;
			return this;
		}
		
		public FulfillmentPlatform build() {
			return new FulfillmentPlatform(this);
		}
	}
}
