package main.org.vikingsoftware.dropshipper.core.data.transaction;

public class Transaction {
	
	public final int id;
	public final TransactionType type;
	public final float amount;
	public final Integer customer_order_id;
	public final Integer processed_order_id;
	public final Integer marketplace_listing_id;
	public final long date;
	public final boolean is_void;
	public final String notes;
	
	private Transaction(final Builder builder) {
		this.id = builder.id;
		this.type = builder.type;
		this.amount = builder.amount;
		this.marketplace_listing_id = builder.marketplace_listing_id;
		this.customer_order_id = builder.customer_order_id;
		this.processed_order_id = builder.processed_order_id;
		this.date = builder.date;
		this.is_void = builder.is_void;
		this.notes = builder.notes;
	}
	
	public static final class Builder {
		
		private int id;
		private TransactionType type;
		private float amount;
		private Integer marketplace_listing_id = null;
		private Integer customer_order_id = null;
		private Integer processed_order_id = null;
		private long date;
		private boolean is_void;
		private String notes;
		
		public Builder id(final int id) {
			this.id = id;
			return this;
		}
		
		public Builder type(final TransactionType type) {
			this.type = type;
			return this;
		}
		
		public Builder amount(final float amount) {
			this.amount = amount;
			return this;
		}
		
		public Builder customer_order_id(final Integer id) {
			this.customer_order_id = id;
			return this;
		}
		
		public Builder processed_order_id(final Integer id) {
			this.processed_order_id = id;
			return this;
		}
		
		public Builder date(final long date) {
			this.date = date;
			return this;
		}
		
		public Builder is_void(final boolean isVoid) {
			this.is_void = isVoid;
			return this;
		}
		
		public Builder marketplace_listing_id(final Integer id) {
			this.marketplace_listing_id = id;
			return this;
		}
		
		public Builder notes(final String notes) {
			this.notes = notes;
			return this;
		}
		
		public Transaction build() {
			return new Transaction(this);
		}
	}
}
