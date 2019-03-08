package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.awt.image.BufferedImage;
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
	public Map<String, BufferedImage> pictures = new HashMap<>();
	public List<PropertyItem> propertyItems = new ArrayList<>();
	public Map<Set<PropertyItem>, Double> variations = new HashMap<>();
}
