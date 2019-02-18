package main.org.vikingsoftware.dropshipper.core.data;

public class CustomerOrder {
	
	/*
	 * SNAKE CASE for easy POJO --> MySQL DB recognition
	 */
	public final int id;
	public final int marketplace_listing_id;
	public final int quantity;
	
	public final String marketplace_order_id;
	public final String buyer_username;
	public final String buyer_name;
	public final String buyer_country;
	public final String buyer_street_address;
	public final String buyer_apt_suite_unit_etc;
	public final String buyer_state_province_region;
	public final String buyer_city;
	public final String buyer_zip_postal_code;
	
	private CustomerOrder(final Builder builder) {
		this.id = builder.id;
		this.marketplace_listing_id = builder.marketplace_listing_id;
		this.quantity = builder.quantity;
		this.marketplace_order_id = builder.marketplace_order_id;
		this.buyer_username = builder.buyer_username;
		this.buyer_name = builder.buyer_name;
		this.buyer_country = builder.buyer_country;
		this.buyer_street_address = builder.buyer_street_address;
		this.buyer_apt_suite_unit_etc = builder.buyer_apt_suite_unit_etc;
		this.buyer_state_province_region = builder.buyer_state_province_region;
		this.buyer_city = builder.buyer_city;
		this.buyer_zip_postal_code = builder.buyer_zip_postal_code;
	}	
	
	public static class Builder {
		private int id;
		private int marketplace_listing_id;
		private int quantity;
		
		private String marketplace_order_id;
		private String buyer_username;
		private String buyer_name;
		private String buyer_country;
		private String buyer_street_address;
		private String buyer_apt_suite_unit_etc;
		private String buyer_state_province_region;
		private String buyer_city;
		private String buyer_zip_postal_code;
		
		public Builder id(final int id) {
			this.id = id;
			return this;
		}
		
		public Builder marketplace_listing_id(final int id) {
			this.marketplace_listing_id = id;
			return this;
		}
		
		public Builder quantity(final int qty) {
			this.quantity = qty;
			return this;
		}
		
		public Builder marketplace_order_id(final String id) {
			this.marketplace_order_id = id;
			return this;
		}
		
		public Builder buyer_username(final String username) {
			this.buyer_username = username;
			return this;
		}
		
		public Builder buyer_name(final String name) {
			this.buyer_name = name;
			return this;
		}
		
		public Builder buyer_country(final String country) {
			this.buyer_country = country;
			return this;
		}
		
		public Builder buyer_street_address(final String address) {
			this.buyer_street_address = address;
			return this;
		}
		
		public Builder buyer_apt_suite_unit_etc(final String apt) {
			this.buyer_apt_suite_unit_etc = apt;
			return this;
		}
		
		public Builder buyer_state_province_region(final String state) {
			this.buyer_state_province_region = state;
			return this;
		}
		
		public Builder buyer_city(final String city) {
			this.buyer_city = city;
			return this;
		}
		
		public Builder buyer_zip_postal_code(final String zip) {
			this.buyer_zip_postal_code = zip;
			return this;
		}
		
		public CustomerOrder build() {
			return new CustomerOrder(this);
		}
	}
}
