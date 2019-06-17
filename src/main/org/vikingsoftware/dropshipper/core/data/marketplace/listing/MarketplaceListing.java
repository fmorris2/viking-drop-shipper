package main.org.vikingsoftware.dropshipper.core.data.marketplace.listing;

import java.sql.Statement;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public class MarketplaceListing {

	public final int id;
	public final int marketplaceId;
	public final String listingId;
	public final String listingTitle;
	public final boolean active;
	public final int current_ebay_inventory;
	public final int fulfillment_quantity_multiplier;

	private MarketplaceListing(final Builder builder) {
		this.id = builder.id;
		this.marketplaceId = builder.marketplaceId;
		this.listingId = builder.listingId;
		this.listingTitle = builder.listingTitle;
		this.active = builder.active;
		this.current_ebay_inventory = builder.current_ebay_inventory;
		this.fulfillment_quantity_multiplier = builder.fulfillment_quantity_multiplier;
	}
	
	public static boolean setIsActive(final String listingId, final boolean isActive) {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("UPDATE marketplace_listing SET active=" + (isActive ? 1 : 0)
					+ " WHERE listing_id="  + listingId);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static boolean setCurrentEbayInventory(final String listingId, final long amount) {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("UPDATE marketplace_listing SET current_ebay_inventory=" + amount
					+ " WHERE listing_id=" + listingId);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static boolean decrementCurrentEbayInventory(final int marketplaceListingId) {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("UPDATE marketplace_listing SET current_ebay_inventory=(current_ebay_inventory-1)"
					+ " WHERE id=" + marketplaceListingId);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean setCurrentEbayInventory(final long amount) {
		return setCurrentEbayInventory(this.listingId, amount);
	}

	public static class Builder {
		private int id;
		private int marketplaceId;
		private String listingId;
		private String listingTitle;
		private boolean active;
		private int current_ebay_inventory;
		private int fulfillment_quantity_multiplier;

		public Builder id(final int id) {
			this.id = id;
			return this;
		}

		public Builder marketplaceId(final int id) {
			this.marketplaceId = id;
			return this;
		}

		public Builder listingId(final String listingId) {
			this.listingId = listingId;
			return this;
		}

		public Builder listingTitle(final String listingTitle) {
			this.listingTitle = listingTitle;
			return this;
		}

		public Builder fulfillmentQuantityMultiplier(final int mult) {
			this.fulfillment_quantity_multiplier = mult;
			return this;
		}
		
		public Builder currentEbayInventory(final int currentInv) {
			this.current_ebay_inventory = currentInv;
			return this;
		}
		
		public Builder active(final boolean active) {
			this.active = active;
			return this;
		}

		public MarketplaceListing build() {
			return new MarketplaceListing(this);
		}
	}
}
