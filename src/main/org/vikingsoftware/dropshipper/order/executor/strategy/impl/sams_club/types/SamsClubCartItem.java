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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cartItemId == null) ? 0 : cartItemId.hashCode());
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		long temp;
		temp = Double.doubleToLongBits(itemCost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + quantity;
		temp = Double.doubleToLongBits(shippingCost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SamsClubCartItem other = (SamsClubCartItem) obj;
		if (cartItemId == null) {
			if (other.cartItemId != null)
				return false;
		} else if (!cartItemId.equals(other.cartItemId))
			return false;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.equals(other.item))
			return false;
		if (Double.doubleToLongBits(itemCost) != Double.doubleToLongBits(other.itemCost))
			return false;
		if (quantity != other.quantity)
			return false;
		if (Double.doubleToLongBits(shippingCost) != Double.doubleToLongBits(other.shippingCost))
			return false;
		return true;
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
