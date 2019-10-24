package main.org.vikingsoftware.dropshipper.core.data.transaction;

public class Transaction {
	
	public final int id;
	public final TransactionType type;
	public final float amount;
	public final Integer customerOrderId;
	public final Integer processedOrderId;
	public final long date;
	
	private Transaction(final Builder builder) {
		this.id = builder.id;
		this.type = builder.type;
		this.amount = builder.amount;
		this.customerOrderId = builder.customerOrderId;
		this.processedOrderId = builder.processedOrderId;
		this.date = builder.date;
	}
	
	public static final class Builder {
		
		private int id;
		private TransactionType type;
		private float amount;
		private Integer customerOrderId = null;
		private Integer processedOrderId = null;
		private long date;
		
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
		
		public Builder customerOrderId(final int id) {
			this.customerOrderId = id;
			return this;
		}
		
		public Builder processedOrderId(final int id) {
			this.processedOrderId = id;
			return this;
		}
		
		public Builder date(final long date) {
			this.date = date;
			return this;
		}
		
		public Transaction build() {
			return new Transaction(this);
		}
	}
}
