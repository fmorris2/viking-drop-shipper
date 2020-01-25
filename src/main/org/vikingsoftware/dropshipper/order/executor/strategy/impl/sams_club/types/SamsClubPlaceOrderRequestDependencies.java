package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

public class SamsClubPlaceOrderRequestDependencies {
	
	public final String paymentId;
	public final double amount;
	
	private SamsClubPlaceOrderRequestDependencies(final Builder builder) {
		this.paymentId = builder.paymentId;
		this.amount = builder.amount;
	}
	
	public static final class Builder {
		private String paymentId;
		private double amount;
		
		public Builder paymentId(final String id) {
			this.paymentId = id;
			return this;
		}
		
		public Builder amount(final double amount) {
			this.amount = amount;
			return this;
		}
		
		public SamsClubPlaceOrderRequestDependencies build() {
			return new SamsClubPlaceOrderRequestDependencies(this);
		}
	}
}
