package main.org.vikingsoftware.dropshipper.core.data.sku;

public class SkuMappingKey {
	private final int marketplaceListingId;
	private final String itemSku;

	public SkuMappingKey(final int id, final String sku) {
		this.marketplaceListingId = id;
		this.itemSku = sku;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemSku == null) ? 0 : itemSku.hashCode());
		result = prime * result + marketplaceListingId;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SkuMappingKey other = (SkuMappingKey) obj;
		if (itemSku == null) {
			if (other.itemSku != null)
				return false;
		} else if (!itemSku.equals(other.itemSku))
			return false;
		if (marketplaceListingId != other.marketplaceListingId)
			return false;
		return true;
	}
}