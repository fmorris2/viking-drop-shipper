package main.org.vikingsoftware.dropshipper.core.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;

public final class SamsClubMetaDataParser {
	
	private static final String META_DATA_PATTERN_STRING = "<script id=\"tb-djs-wml-redux-state\" type=\"application/json\">(.+);<\\/script>";
	private static final Pattern META_DATA_PATTERN = Pattern.compile(META_DATA_PATTERN_STRING);
	
	private static final String INVALID_IMAGE_URL = "https://scene7.samsclub.com/is/image/samsclub/fgsdfgsdgfsdgds";
	private static final BufferedImage notFoundImage = ImageUtils.getImageFromUrl(INVALID_IMAGE_URL);
	
	private final JsonParser parser = new JsonParser();
	
	private JsonObject metaData;
	private JsonObject internalProduct;
	
	public static void main(final String[] args) throws IOException {
		final SamsClubMetaDataParser parser = new SamsClubMetaDataParser();
		int stock = -1;
		for(int i = 0; i < 1000; i++) {
			parser.parse(Jsoup.connect("https://www.samsclub.com/p/mm-fluticasone-6x120-sprays/prod21003064")
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36")
					.get()
					.html());
			System.out.println(parser.getStock());
			if(stock == -1) {
				stock = parser.getStock();
			}
			
			if(stock != parser.getStock()) {
				System.err.println("FAILED");
				System.exit(0);
			}
		}
	}
	
	public boolean parse(final String pageSource) {
		try {
			final String jsonString = getMetaDataJsonString(pageSource);
			final JsonReader reader = new JsonReader(new StringReader(jsonString));
			reader.setLenient(true);
			this.metaData = parser.parse(reader)
					.getAsJsonObject()
					.get("product")
					.getAsJsonObject();
			this.internalProduct = this.metaData.get("product").getAsJsonObject();
			return true;
		} catch(final Exception e) {
			//swallow exception
		}
		
		return false;
	}
	
	public String getProductName() {
		return internalProduct.get("title").getAsString();
	}
	
	public String getItemID() {
		final JsonElement itemNumberEl = internalProduct == null ? null : internalProduct.get("itemNumber");
		return itemNumberEl != null && !itemNumberEl.isJsonNull() ? itemNumberEl.getAsString() : null;
	}
	
	public String getProductID() {
		try {
			return internalProduct == null ? null : internalProduct.get("productId").getAsString();
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String getDescription() {
		return metaData.get("description")
				.getAsJsonObject()
				.get("longDescription")
				.getAsString();
	}
	
	public Optional<Double> getPrice() {
		final JsonElement selectedSkuEl = metaData.get("selectedSku");
		if(selectedSkuEl != null && !selectedSkuEl.isJsonNull()) {
			final JsonObject selectedSkuObj = selectedSkuEl.getAsJsonObject();
			final JsonElement pricingOptionsEl = selectedSkuObj.get("pricingOptions");
			if(pricingOptionsEl != null && !pricingOptionsEl.isJsonNull()) {
				final JsonArray pricingOptionsArr = pricingOptionsEl.getAsJsonArray();
				if(pricingOptionsArr.size() > 0) {
					return Optional.of(pricingOptionsArr
							.get(0)
							.getAsJsonObject()
							.get("listPrice")
							.getAsJsonObject()
							.get("currencyAmount")
							.getAsDouble());
				}
			}
		}
		return Optional.empty();
	}
	
	public int getStock() {
		try {
			final int stock = internalProduct.get("onlineInventory")
					.getAsJsonObject()
					.get("availableToSellQuantity")
					.getAsInt();
			
			return stock;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	public boolean passesAllListingConditions() {
		try {
			return isFreeShipping() && isAvailableOnline()
					&& !hasMinPurchaseQty() && !hasVariations();
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean isFreeShipping() {
		final JsonObject moneybox = metaData.get("moneybox").getAsJsonObject();
		
		return moneybox.get("shippingIncluded").getAsBoolean()
				|| moneybox.get("shippingFree").getAsBoolean()
				|| moneybox.get("shippingFreeEligible").getAsBoolean();
	}
	
	public String getBrand() {
		final JsonElement brand = internalProduct.get("brandName");
		return brand == null ? "Unbranded" : brand.getAsString();
	}
	
	public boolean isAvailableOnline() {
		return internalProduct.has("inStockOnline") && internalProduct.get("inStockOnline").getAsBoolean();
	}
	
	public boolean hasMinPurchaseQty() {
		return internalProduct.get("onlineInventory")
				.getAsJsonObject()
				.get("minPurchaseQuantity")
				.getAsInt() > 1;
	}
	
	public boolean hasVariations() {
		return internalProduct.get("skuOptions").getAsJsonArray().size() > 1;
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
			final String adjustedResolutionImageUrl = imageUrl.contains("$DT_Zoom$") ? imageUrl : imageUrl + "$DT_Zoom$";
			images.add(new ListingImage(adjustedResolutionImageUrl));
			
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
