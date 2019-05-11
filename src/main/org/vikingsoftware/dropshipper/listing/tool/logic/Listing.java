package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

public class Listing implements Cloneable {
	public String title;
	public String description;
	public EbayCategory category;
	public String itemId;
	public String brand;
	public int fulfillmentPlatformId;
	public double price;
	public ShippingServiceCodeType shippingService;
	public double shipping;
	public double targetProfitMargin;
	public List<ListingImage> pictures = new ArrayList<>();
	public List<PropertyItem> propertyItems = new ArrayList<>();
	public Map<Set<PropertyItemOption>, Double> variations = new HashMap<>();
	public boolean canShip = false;

	@Override
	public Listing clone() {
		final Listing clone = new Listing();
		clone.title = title;
		clone.description = description;
		clone.itemId = itemId;
		clone.brand = brand;
		clone.fulfillmentPlatformId = fulfillmentPlatformId;
		clone.price = price;
		clone.shippingService = shippingService;
		clone.shipping = shipping;
		clone.pictures = new ArrayList<>(pictures);
		clone.propertyItems = new ArrayList<>(propertyItems);
		clone.variations = new HashMap<>(variations);
		clone.canShip = canShip;
		return clone;
	}
}
