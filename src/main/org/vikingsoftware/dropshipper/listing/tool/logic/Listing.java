package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Listing {
	public String title;
	public String description;
	public String categoryId;
	public String categoryName;
	public double price;
	public List<ListingImage> pictures = new ArrayList<>();
	public List<PropertyItem> propertyItems = new ArrayList<>();
	public Map<Set<PropertyItemOption>, Double> variations = new HashMap<>();
}
