package main.org.vikingsoftware.dropshipper.core.data.transaction;

public enum TransactionType {
	
	MARKETPLACE_INCOME(0),
	MARKETPLACE_SELL_FEE(1),
	FULFILLMENT_COST(2),
	MARKETPLACE_REFUND(3),
	FULFILLMENT_REFUND(4),
	CUSTOMER_SATISFACTION(5),
	FULFILLMENT_PLATFORM_OPERATING_COST(6),
	MARKETPLACE_OPERATING_COST(7);
	
	public final int value;
	
	TransactionType(final int value) {
		this.value = value;
	}
}
