package main.org.vikingsoftware.dropshipper.core.data.processed.order;

public class ProcessedOrder {
	
	public final int customer_order_id;
	public final int fulfillment_listing_id;
	public final String fulfillment_transaction_id;
	public final double sale_price;
	public final int quantity;
	public final String order_status;
	
	private ProcessedOrder(final Builder builder) {
		this.customer_order_id = builder.customer_order_id;
		this.fulfillment_listing_id = builder.fulfillment_listing_id;
		this.fulfillment_transaction_id = builder.fulfillment_transaction_id;
		this.sale_price = builder.sale_price;
		this.quantity = builder.quantity;
		this.order_status = builder.order_status;
	}
	
	public static class Builder {
		private int customer_order_id;
		private int fulfillment_listing_id;
		private String fulfillment_transaction_id;
		private double sale_price;
		private int quantity;
		private String order_status;
		
		public Builder customer_order_id(final int id) {
			this.customer_order_id = id;
			return this;
		}
		
		public Builder fulfillment_listing_id(final int id) {
			this.fulfillment_listing_id = id;
			return this;
		}
		
		public Builder fulfillment_transaction_id(final String id) {
			this.fulfillment_transaction_id = id;
			return this;
		}
		
		public Builder sale_price(final double price) {
			this.sale_price = price;
			return this;
		}
		
		public Builder quantity(final int quantity) {
			this.quantity = quantity;
			return this;
		}
		
		public Builder order_status(final String status) {
			this.order_status = status;
			return this;
		}
		
		public ProcessedOrder build() {
			return new ProcessedOrder(this);
		}
	}
	
}
