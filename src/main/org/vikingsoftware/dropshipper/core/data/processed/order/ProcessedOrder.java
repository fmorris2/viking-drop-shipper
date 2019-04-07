package main.org.vikingsoftware.dropshipper.core.data.processed.order;

public class ProcessedOrder {

	public final int id;
	public final int customer_order_id;
	public final int fulfillment_listing_id;
	public final int fulfillment_account_id;
	public final String fulfillment_transaction_id;
	public final String tracking_number;
	public final double sale_price;

	private ProcessedOrder(final Builder builder) {
		this.id = builder.id;
		this.customer_order_id = builder.customer_order_id;
		this.fulfillment_listing_id = builder.fulfillment_listing_id;
		this.fulfillment_account_id = builder.fulfillment_account_id;
		this.fulfillment_transaction_id = builder.fulfillment_transaction_id;
		this.tracking_number = builder.tracking_number;
		this.sale_price = builder.sale_price;
	}

	public static class Builder {
		private int id;
		private int customer_order_id;
		private int fulfillment_listing_id;
		private int fulfillment_account_id;
		private String fulfillment_transaction_id;
		private String tracking_number;
		private double sale_price;

		public Builder id(final int id) {
			this.id = id;
			return this;
		}

		public Builder customer_order_id(final int id) {
			this.customer_order_id = id;
			return this;
		}

		public Builder fulfillment_listing_id(final int id) {
			this.fulfillment_listing_id = id;
			return this;
		}

		public Builder fulfillment_account_id(final int id) {
			this.fulfillment_account_id = id;
			return this;
		}

		public Builder fulfillment_transaction_id(final String id) {
			this.fulfillment_transaction_id = id;
			return this;
		}

		public Builder tracking_number(final String num) {
			this.tracking_number = num;
			return this;
		}

		public Builder sale_price(final double price) {
			this.sale_price = price;
			return this;
		}

		public ProcessedOrder build() {
			return new ProcessedOrder(this);
		}
	}

}
