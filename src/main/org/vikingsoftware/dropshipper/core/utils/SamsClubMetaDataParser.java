package main.org.vikingsoftware.dropshipper.core.utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;

public final class SamsClubMetaDataParser {
	
	private static final String META_DATA_PATTERN_STRING = "window.__WML_REDUX_INITIAL_STATE__ = (.+);<\\/script>";
	private static final Pattern META_DATA_PATTERN = Pattern.compile(META_DATA_PATTERN_STRING);
	
	private static final String INVALID_IMAGE_URL = "https://scene7.samsclub.com/is/image/samsclub/fgsdfgsdgfsdgds";
	private static final BufferedImage notFoundImage = ImageUtils.getImageFromUrl(INVALID_IMAGE_URL);
	
	private static final JsonParser parser = new JsonParser();
	
	private JsonObject metaData;
	private JsonObject internalProduct;
	
	public void parse(final String pageSource) {
		final String jsonString = getMetaDataJsonString(pageSource);
		this.metaData = parser.parse(jsonString)
				.getAsJsonObject()
				.get("product")
				.getAsJsonObject();
		this.internalProduct = this.metaData.get("product").getAsJsonObject();
	}
	
	public String getProductName() {
		return internalProduct.get("title").getAsString();
	}
	
	public String getItemID() {
		return internalProduct.get("itemNumber").getAsString();
	}
	
	public String getDescription() {
		return metaData.get("description")
				.getAsJsonObject()
				.get("longDescription")
				.getAsString();
	}
	
	public double getPrice() {
		return metaData.get("selectedSku")
				.getAsJsonObject()
				.get("pricingOptions")
				.getAsJsonArray()
				.get(0)
				.getAsJsonObject()
				.get("finalPrice")
				.getAsJsonObject()
				.get("currencyAmount")
				.getAsDouble();
	}
	
	public String getBrand() {
		return internalProduct.get("brandName").getAsString();
	}
	
	public boolean isAvailableOnline() {
		return internalProduct.has("inStockOnline") && internalProduct.get("inStockOnline").getAsBoolean();
	}
	
	public List<ListingImage> getImages() {
		final List<ListingImage> images = new ArrayList<>();
		
		String imageUrl = metaData.get("selectedSku")
				.getAsJsonObject()
				.get("imageUrl")
				.getAsString();
		
		BufferedImage currentImage = ImageUtils.getImageFromUrl(imageUrl);
		double imgDifferencePercent = 0.0;
		char currentSuffix = imageUrl.charAt(imageUrl.length() - 1);
		
		while((imgDifferencePercent = ImageUtils.getDifferencePercent(currentImage, notFoundImage)) > 0) {
			System.out.println("currentSuffix: " + currentSuffix + ", imgDifferencePercent = " + imgDifferencePercent);
			images.add(new ListingImage(imageUrl, currentImage));
			
			currentSuffix = (char)(((int)currentSuffix) + 1);
			imageUrl = imageUrl.substring(0, imageUrl.length() - 1) + currentSuffix;
			currentImage = ImageUtils.getImageFromUrl(imageUrl);
		}
		
		return images;
	}
	
	private String getMetaDataJsonString(final String pageSource) {
		final Matcher matcher = META_DATA_PATTERN.matcher(pageSource);
		if(matcher.find()) {
			return matcher.group(1);
		}
		
		return null;
	}
}