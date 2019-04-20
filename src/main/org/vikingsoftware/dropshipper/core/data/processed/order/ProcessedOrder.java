package main.org.vikingsoftware.dropshipper.core.data.processed.order;

public class ProcessedOrder {

	public final int id;
	public final int customer_order_id;
	public final int fulfillment_listing_id;
	public final int fulfillment_account_id;
	public final String fulfillment_transaction_id;
	public final String tracking_number;
	public final double buy_subtotal;
	public final double buy_sales_tax;
	public final double buy_shipping;
	public final double buy_product_fees;
	public final double buy_total;
	public final double profit;

	private ProcessedOrder(final Builder builder) {
		this.id = builder.id;
		this.customer_order_id = builder.customer_order_id;
		this.fulfillment_listing_id = builder.fulfillment_listing_id;
		this.fulfillment_account_id = builder.fulfillment_account_id;
		this.fulfillment_transaction_id = builder.fulfillment_transaction_id;
		this.tracking_number = builder.tracking_number;
		this.buy_subtotal = builder.buy_subtotal;
		this.buy_sales_tax = builder.buy_sales_tax;
		this.buy_shipping = builder.buy_shipping;
		this.buy_product_fees = builder.buy_product_fees;
		this.buy_total = builder.buy_total;
		this.profit = builder.profit;
	}

	public static class Builder {
		private int id;
		private int customer_order_id;
		private int fulfillment_listing_id;
		private int fulfillment_account_id;
		private String fulfillment_transaction_id;
		private String tracking_number;
		private double buy_subtotal;
		private double buy_sales_tax;
		private double buy_shipping;
		private double buy_product_fees;
		private double buy_total;
		private double profit;

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

		public Builder buy_total(final double val) {
			this.buy_total = val;
			return this;
		}

		public Builder buy_product_fees(final double val) {
			this.buy_product_fees = val;
			return this;
		}

		public Builder buy_shipping(final double val) {
			this.buy_shipping = val;
			return this;
		}

		public Builder buy_sales_tax(final double val) {
			this.buy_sales_tax = val;
			return this;
		}

		public Builder buy_subtotal(final double val) {
			this.buy_subtotal = val;
			return this;
		}

		public Builder profit(final double val) {
			this.profit = val;
			return this;
		}

		public ProcessedOrder build() {
			return new ProcessedOrder(this);
		}
	}

}
