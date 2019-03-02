package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

public class FulfillmentPlatform {
	public final int id;
	public final String platform_name;
	public final String platform_url;
	public final boolean frozen;
	
	private FulfillmentPlatform(final Builder builder) {
		this.id = builder.id;
		this.platform_name = builder.platform_name;
		this.platform_url = builder.platform_url;
		this.frozen = builder.frozen;
	}
	
	public static class Builder {
		private int id;
		private String platform_name;
		private String platform_url;
		private boolean frozen;
		
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
		
		public Builder frozen(final boolean frozen) {
			this.frozen = frozen;
			return this;
		}
		
		public FulfillmentPlatform build() {
			return new FulfillmentPlatform(this);
		}
	}
}
