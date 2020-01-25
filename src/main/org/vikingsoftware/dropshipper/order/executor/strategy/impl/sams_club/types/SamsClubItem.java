package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

public class SamsClubItem {
	
	public final String itemNumber;
	public final String productId;
	public final String skuId;
	
	public SamsClubItem(final String itemNumber, final String productId, final String skuId) {
		this.itemNumber = itemNumber;
		this.productId = productId;
		this.skuId = skuId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemNumber == null) ? 0 : itemNumber.hashCode());
		result = prime * result + ((productId == null) ? 0 : productId.hashCode());
		result = prime * result + ((skuId == null) ? 0 : skuId.hashCode());
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
		SamsClubItem other = (SamsClubItem) obj;
		if (itemNumber == null) {
			if (other.itemNumber != null)
				return false;
		} else if (!itemNumber.equals(other.itemNumber))
			return false;
		if (productId == null) {
			if (other.productId != null)
				return false;
		} else if (!productId.equals(other.productId))
			return false;
		if (skuId == null) {
			if (other.skuId != null)
				return false;
		} else if (!skuId.equals(other.skuId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SamsClubItem:\n"
				+ "\t\tItem Number: " + itemNumber + "\n"
				+ "\t\tProduct ID: " + productId + "\n"
				+ "\t\tSku ID: " + skuId;
	}
}
