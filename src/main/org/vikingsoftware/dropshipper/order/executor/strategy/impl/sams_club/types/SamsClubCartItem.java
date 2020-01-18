package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

public class SamsClubCartItem {
	
	public final String cartItemId;
	public final SamsClubItem item;
	public final int quantity;
	public final double itemCost; //cost of the item - doesn't include tax
	public final double shippingCost;
	
	private SamsClubCartItem(final Builder builder) {
		this.cartItemId = builder.cartItemId;
		this.item = builder.item;
		this.quantity = builder.quantity;
		this.itemCost = builder.itemCost;
		this.shippingCost = builder.shippingCost;
	}
	
	@Override
	public String toString() {
		return "SamsClubCartItem:\n"
				+ "\tCart Item ID: " + cartItemId + "\n"
				+ "\t" + item + "\n"
				+ "\tQuantity: " + quantity + "\n"
				+ "\tItem Cost: " + itemCost + "\n"
				+ "\tShipping Cost: " + shippingCost;
	}
	
	public static final class Builder {
		private String cartItemId;
		private SamsClubItem item;
		private int quantity;
		private double itemCost;
		private double shippingCost;
		
		public Builder cartItemId(final String id) {
			this.cartItemId = id;
			return this;
		}
		
		public Builder item(final SamsClubItem item) {
			this.item = item;
			return this;
		}
		
		public Builder quantity(final int quantity) {
			this.quantity = quantity;
			return this;
		}
		
		public Builder itemCost(final double cost) {
			this.itemCost = cost;
			return this;
		}
		
		public Builder shippingCost(final double cost) {
			this.shippingCost = cost;
			return this;
		}
		
		public SamsClubCartItem build() {
			return new SamsClubCartItem(this);
		}
	}
}
