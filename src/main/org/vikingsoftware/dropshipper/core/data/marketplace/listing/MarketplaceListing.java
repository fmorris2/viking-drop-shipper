package main.org.vikingsoftware.dropshipper.core.data.marketplace.listing;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplace;
import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;

public class MarketplaceListing {

	public final int id;
	public final int marketplaceId;
	public final String listingId;
	public final String listingTitle;
	public final boolean active;
	public final int current_ebay_inventory;
	public final double target_margin;
	public final int fulfillment_quantity_multiplier;
	public final int current_handling_time;
	public final int target_handling_time;
	
	private double current_price;
	private double current_shipping_cost;

	private MarketplaceListing(final Builder builder) {
		this.id = builder.id;
		this.marketplaceId = builder.marketplaceId;
		this.listingId = builder.listingId;
		this.listingTitle = builder.listingTitle;
		this.active = builder.active;
		this.current_ebay_inventory = builder.current_ebay_inventory;
		this.target_margin = builder.target_margin;
		this.current_price = builder.current_price;
		this.current_shipping_cost = builder.current_shipping_cost;
		this.fulfillment_quantity_multiplier = builder.fulfillment_quantity_multiplier;
		this.current_handling_time = builder.current_handling_time;
		this.target_handling_time = builder.target_handling_time;
	}
	
	public static boolean setIsActive(final String listingId, final boolean isActive) {
		return flipBoolean(listingId, "active", isActive);
	}
	
	public static boolean setIsPurged(final String listingId, final boolean isPurged) {
		return flipBoolean(listingId, "is_purged", isPurged);
	}
	
	public static boolean flipBoolean(final String listingId, final String columnName, final boolean isActive) {
		final String sql = "UPDATE marketplace_listing SET " + columnName + " = ? WHERE listing_id = ?";
		try (final PreparedStatement st = VSDSDBManager.get().createPreparedStatement(sql)) {
			st.setInt(1, (isActive ? 1 : 0));
			st.setString(2, listingId);
			st.execute();
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static boolean setCurrentEbayInventory(final String listingId, final long amount) {
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			st.execute("UPDATE marketplace_listing SET current_ebay_inventory=" + amount
					+ " WHERE listing_id=" + listingId);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static boolean flagForPurgeExamination(final int listingId) {
		System.out.println("Flagging for purge examination: " + listingId);
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			final long ms = System.currentTimeMillis();
			st.execute("UPDATE marketplace_listing SET needs_purge_examination=1,last_margin_update="+ms
					+ " WHERE id=" + listingId);
			System.out.println("\tsuccess.");
			return true;
		} catch(final Exception e) {
			System.out.println("\tfailure.");
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static boolean decrementCurrentEbayInventory(final int marketplaceListingId) {
		try (final Statement st = VSDSDBManager.get().createStatement()) {
			st.execute("UPDATE marketplace_listing SET current_ebay_inventory=(current_ebay_inventory-1)"
					+ " WHERE id=" + marketplaceListingId);
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public Pair<Double, Double> getCurrentPrice() throws Exception {
		/*
		 * Only call eBay API for current listing price if we don't already have it stored in the DB.
		 * This will only be called once for every listing on creation.
		 */
		if(current_price == 0) {
			final Pair<Double, Double> priceInfo = EbayCalls.getPrice(listingId);
			current_price = priceInfo.left;
			current_shipping_cost = priceInfo.right;
			
			//update our DB
			try(final Statement st = VSDSDBManager.get().createStatement()) {
				st.execute("UPDATE marketplace_listing SET current_price="+current_price
					+ ",current_shipping_cost="+current_shipping_cost+" WHERE id="+id);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
		
		return new Pair<>(current_price, current_shipping_cost);
	}
	
	public static Optional<Integer> getCurrentHandlingTime(final int listingId) {
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res =st.executeQuery("SELECT current_handling_time FROM marketplace_listing WHERE id="+listingId)) {
			if(res.next()) {
				return Optional.of(res.getInt("current_handling_time"));
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return Optional.empty();
	}
	
	public static Optional<Integer> getFulfillmentQuantityMultiplier(final int listingId) {
		try(final Statement st = VSDSDBManager.get().createStatement();
				final ResultSet res =st.executeQuery("SELECT fulfillment_quantity_multiplier FROM marketplace_listing WHERE id="+listingId)) {
				if(res.next()) {
					return Optional.of(res.getInt("fulfillment_quantity_multiplier"));
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}
			
			return Optional.empty();
	}
	
	public void updatePrice(final double newPrice) throws SQLException {
		if(EbayCalls.updatePrice(listingId, newPrice)) {
			try(final Statement st = VSDSDBManager.get().createStatement()) {
				st.execute("UPDATE marketplace_listing SET current_price="+newPrice
					+ " WHERE id="+id);
			}
		}
	}
	
	public void updateHandlingTimeInDB(final int handlingTime) {
		try(final Statement st = VSDSDBManager.get().createStatement()) {
			st.execute("UPDATE marketplace_listing SET current_handling_time="+handlingTime
					+ " WHERE id="+id);
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public static MarketplaceListing getMarketplaceListingForFulfillmentListing(final int fulfillmentListingId) {
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet initial = st.executeQuery("SELECT marketplace_listing_id FROM fulfillment_mapping WHERE"
					+ " fulfillment_listing_id="+fulfillmentListingId)) { // you GOOD??
			if(initial.next()) {
				final int marketplaceListingId = initial.getInt("marketplace_listing_id");
				try(final Statement st2 = VSDSDBManager.get().createStatement();
					final ResultSet rs2 = st2.executeQuery("SELECT * FROM marketplace_listing WHERE id=" + marketplaceListingId)) {
					if(rs2.next()) {
						return Marketplace.loadListingFromResultSet(rs2);
					}
				}
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
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
		private double target_margin;
		private double current_price;
		private double current_shipping_cost;
		private int fulfillment_quantity_multiplier;	
		private int current_handling_time;
		private int target_handling_time;

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
		
		public Builder targetMargin(final double margin) {
			this.target_margin = margin;
			return this;
		}
		
		public Builder currentPrice(final double price) {
			this.current_price = price;
			return this;
		}
		
		public Builder currentShippingCost(final double shipping) {
			this.current_shipping_cost = shipping;
			return this;
		}
		
		public Builder active(final boolean active) {
			this.active = active;
			return this;
		}
		
		public Builder current_handling_time(final int time) {
			this.current_handling_time = time;
			return this;
		}
		
		public Builder target_handling_time(final int time) {
			this.target_handling_time = time;
			return this;
		}

		public MarketplaceListing build() {
			return new MarketplaceListing(this);
		}
	}
}
