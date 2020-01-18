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
	public String toString() {
		return "SamsClubItem:\n"
				+ "\t\tItem Number: " + itemNumber + "\n"
				+ "\t\tProduct ID: " + productId + "\n"
				+ "\t\tSku ID: " + skuId;
	}
}
