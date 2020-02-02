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
	public Map<String, List<String>> requiredItemSpecifics;
	public Map<String, String> itemSpecifics = new HashMap<>();
	public String itemId;
	public String productId;
	public String skuId;
	public String brand;
	public String url;
	public String upc;
	public String ean;
	public String mpn;
	public int fulfillmentPlatformId;
	public int handlingTime = 2;
	public int minPurchaseQty = 1;
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
		clone.category = category;
		clone.requiredItemSpecifics = requiredItemSpecifics;
		clone.itemSpecifics = itemSpecifics;
		clone.description = description;
		clone.itemId = itemId;
		clone.productId = productId;
		clone.skuId = skuId;
		clone.brand = brand;
		clone.url = url;
		clone.fulfillmentPlatformId = fulfillmentPlatformId;
		clone.price = price;
		clone.shippingService = shippingService;
		clone.shipping = shipping;
		clone.pictures = new ArrayList<>(pictures);
		clone.propertyItems = new ArrayList<>(propertyItems);
		clone.variations = new HashMap<>(variations);
		clone.canShip = canShip;
		clone.upc = upc;
		clone.ean = ean;
		clone.mpn = mpn;
		clone.handlingTime = handlingTime;
		clone.minPurchaseQty = minPurchaseQty;
		return clone;
	}
	
	@Override
	public String toString() {
		return "Listing:\n"
				+ "\tTitle: " + this.title + "\n"
				//+ "\tDescription: " + this.description + "\n"
				+ "\tCategory: " + (this.category == null ? "N/A" : this.category.name) + "\n"
				+ "\tItem Specifics: " + this.itemSpecifics + "\n"
				+ "\tItem Id: " + this.itemId + "\n"
				+ "\tProduct Id: " + this.productId + "\n"
				+ "\tBrand: " + this.brand + "\n"
				+ "\tUrl: " + this.url + "\n"
				+ "\tUPC: " + this.upc + "\n"
				+ "\tEAN: " + this.ean + "\n"
				+ "\tMPN: " + this.mpn + "\n"
				+ "\tSKU Id: " + this.skuId + "\n"
				+ "\tPrice: " + this.price + "\n"
				+ "\tShipping: " + this.shipping + "\n"
				+ "\tNum Pictures: " + this.pictures.size()
				+ "\tMin Purchase Qty: " + this.minPurchaseQty;
	}
}
