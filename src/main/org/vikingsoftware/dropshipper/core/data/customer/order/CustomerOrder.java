package main.org.vikingsoftware.dropshipper.core.data.customer.order;

import java.text.DecimalFormat;
import java.text.Normalizer;


public class CustomerOrder {

	/*
	 * SNAKE CASE for easy POJO --> MySQL DB recognition
	 */
	public final int id;
	public final int marketplace_listing_id;
	public final String sku;
	public final double sell_listing_price;
	public final double sell_shipping;
	public final double sell_percentage_cut;
	public final double sell_total;
	public final int quantity;
	public final int fulfillment_purchase_quantity;
	public final int handling_time;

	public final String marketplace_order_id;
	public final String buyer_username;
	public final String buyer_name, normalizedBuyerName;
	public final String buyer_country;
	public final String buyer_street_address;
	public final String buyer_apt_suite_unit_etc;
	public final String buyer_state_province_region;
	public final String buyer_city;
	public final String buyer_zip_postal_code;
	public final String buyer_phone_number;
	public final long date_parsed;
	public final long date_cancelled;
	public final boolean is_cancelled;
	
	public final Double marketplace_sell_fee;

	private CustomerOrder(final Builder builder) {
		this.id = builder.id;
		this.marketplace_listing_id = builder.marketplace_listing_id;
		this.sku = builder.sku;
		this.sell_listing_price = builder.sell_listing_price;
		this.sell_shipping = builder.sell_shipping;
		this.sell_percentage_cut = builder.sell_percentage_cut;
		this.sell_total = builder.sell_total;
		this.quantity = builder.quantity;
		this.fulfillment_purchase_quantity = builder.fulfillment_purchase_quantity;
		this.marketplace_order_id = builder.marketplace_order_id;
		this.buyer_username = builder.buyer_username;
		this.buyer_name = builder.buyer_name.replaceAll(" {2,}", " ").replace(".", "").replace(",", "");
		this.normalizedBuyerName = Normalizer
				.normalize(buyer_name, Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", "");
		this.buyer_country = builder.buyer_country;
		this.buyer_street_address = builder.buyer_street_address;
		this.buyer_apt_suite_unit_etc = builder.buyer_apt_suite_unit_etc;
		this.buyer_state_province_region = builder.buyer_state_province_region;
		this.buyer_city = builder.buyer_city;
		this.buyer_zip_postal_code = builder.buyer_zip_postal_code;
		this.buyer_phone_number = builder.buyer_phone_number;
		this.date_parsed = builder.date_parsed;
		this.date_cancelled = builder.date_cancelled;
		this.is_cancelled = builder.is_cancelled;
		this.marketplace_sell_fee = builder.marketplace_sell_fee;
		this.handling_time = builder.handling_time;
	}
	
	public String getFirstName() {
		return buyer_name.split(" ")[0];
	}

	public String getLastName() {
		return buyer_name.substring(getFirstName().length() + 1);
	}

	public double getProfit(final double totalFulfillmentPrice) {
		final double profit = (sell_total * .87) - totalFulfillmentPrice;
		final DecimalFormat format = new DecimalFormat("###.##");
		return Double.parseDouble(format.format(profit));
	}

	public static class Builder {
		private int id;
		private int marketplace_listing_id;
		private String sku;
		private double sell_listing_price;
		private double sell_shipping;
		private double sell_percentage_cut;
		private double sell_total;
		private int quantity;
		private int fulfillment_purchase_quantity;
		private int handling_time;

		private String marketplace_order_id;
		private String buyer_username;
		private String buyer_name;
		private String buyer_country;
		private String buyer_street_address;
		private String buyer_apt_suite_unit_etc;
		private String buyer_state_province_region;
		private String buyer_city;
		private String buyer_zip_postal_code;
		private String buyer_phone_number;
		private long date_parsed;
		private long date_cancelled;
		private boolean is_cancelled;
		
		private Double marketplace_sell_fee;

		public Builder id(final int id) {
			this.id = id;
			return this;
		}

		public Builder marketplace_listing_id(final int id) {
			this.marketplace_listing_id = id;
			return this;
		}

		public Builder sku(final String sku) {
			this.sku = sku;
			return this;
		}

		public Builder sell_shipping(final double val) {
			this.sell_shipping = val;
			return this;
		}

		public Builder sell_percentage_cut(final double val) {
			this.sell_percentage_cut = val;
			return this;
		}

		public Builder sell_total(final double val) {
			this.sell_total = val;
			return this;
		}

		public Builder sell_listing_price(final double val) {
			this.sell_listing_price = val;
			return this;
		}

		public Builder quantity(final int qty) {
			this.quantity = qty;
			return this;
		}

		public Builder fulfillment_purchase_quantity(final int qty) {
			this.fulfillment_purchase_quantity = qty;
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

		public Builder buyer_phone_number(final String num) {
			this.buyer_phone_number = num;
			return this;
		}
		
		public Builder date_parsed(final long timestamp) {
			this.date_parsed = timestamp;
			return this;
		}
		
		public Builder date_cancelled(final long val) {
			this.date_cancelled = val;
			return this;
		}
		
		public Builder is_cancelled(final boolean val) {
			this.is_cancelled = val;
			return this;
		}
		
		public Builder marketplace_sell_fee(final double fee) {
			this.marketplace_sell_fee = fee;
			return this;
		}
		
		public Builder handling_time(final int time) {
			this.handling_time = time;
			return this;
		}

		public CustomerOrder build() {
			return new CustomerOrder(this);
		}
	}
}
