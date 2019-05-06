package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Listing implements Cloneable {
	public String title;
	public String description;
	public String categoryId;
	public String categoryName;
	public double price;
	public List<ListingImage> pictures = new ArrayList<>();
	public List<PropertyItem> propertyItems = new ArrayList<>();
	public Map<Set<PropertyItemOption>, Double> variations = new HashMap<>();

	@Override
	public Listing clone() {
		final Listing clone = new Listing();
		clone.title = title;
		clone.description = description;
		clone.categoryId = categoryId;
		clone.categoryName = categoryName;
		clone.price = price;
		clone.pictures = new ArrayList<>(pictures);
		clone.propertyItems = new ArrayList<>(propertyItems);
		clone.variations = new HashMap<>(variations);
		return clone;
	}
}
