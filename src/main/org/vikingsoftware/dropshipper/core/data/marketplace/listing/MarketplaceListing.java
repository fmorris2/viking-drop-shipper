package main.org.vikingsoftware.dropshipper.core.data.marketplace.listing;

import java.sql.Statement;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public class MarketplaceListing {

	public final int id;
	public final int marketplaceId;
	public final String listingId;
	public final String listingTitle;
	public final boolean active;
	public final int last_inventory_update;
	public final int fulfillment_quantity_multiplier;

	private MarketplaceListing(final Builder builder) {
		this.id = builder.id;
		this.marketplaceId = builder.marketplaceId;
		this.listingId = builder.listingId;
		this.listingTitle = builder.listingTitle;
		this.active = builder.active;
		this.last_inventory_update = builder.last_inventory_update;
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
	
	public boolean setLastInventoryUpdate(final long amount) {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			st.execute("UPDATE marketplace_listing SET last_inventory_update=" + amount
					+ " WHERE id=" + this.id);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	public static class Builder {
		private int id;
		private int marketplaceId;
		private String listingId;
		private String listingTitle;
		private boolean active;
		private int last_inventory_update;
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
		
		public Builder last_inventory_update(final int lastUpdate) {
			this.last_inventory_update = lastUpdate;
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
