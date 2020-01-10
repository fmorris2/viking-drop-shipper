package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.utils.ImageUtils;
import main.org.vikingsoftware.dropshipper.core.utils.UPCUtils;
import main.org.vikingsoftware.dropshipper.core.web.JsonAPIParser;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;

public final class SamsProductAPI extends JsonAPIParser {
	
	private static final String API_BASE_URL = "https://www.samsclub.com/api/soa/services/v1/catalog/product/";
	private static final String API_URL_ARGS = "?response_group=LARGE&clubId=6279";
	
	private static final String INVALID_IMAGE_URL = "https://scene7.samsclub.com/is/image/samsclub/fgsdfgsdgfsdgds";
	private static final BufferedImage notFoundImage = ImageUtils.getImageFromUrl(INVALID_IMAGE_URL);
	
	private Optional<JSONObject> payload;
	private Optional<JSONObject> onlineInventory;
	private Optional<JSONObject> onlinePricing;
	private Optional<JSONArray>  skuOptionsArr;
	private Optional<JSONObject> skuOptions;
	
	private String productId;
	
	public static void main(final String[] args) {
		final SamsProductAPI api = new SamsProductAPI();
		api.parse("prod21220949");
		System.out.println("Current Stock: " + api.getAvailableToSellQuantity().orElse(0));
		System.out.println("List Price: " + api.getListPrice().orElse(0D));
		System.out.println("UPC: " + api.getUPC().orElse(null));
		System.out.println("Passes all conditions: " + api.passesAllListingConditions());
		System.out.println("\tisFreeShipping: " + api.isFreeShipping());
		System.out.println("\tisAvailableOnline: " + api.isAvailableOnline());
		System.out.println("\thasMinPurchaseQty: " + api.hasMinPurchaseQty());
		System.out.println("\thasVariations: " + api.hasVariations());
	}
	
	public boolean parse(final String productId) {
		String apiUrl = null;
		try {
			reset();
			this.productId = productId;
			apiUrl = API_BASE_URL + productId + API_URL_ARGS;
			final URL urlObj = new URL(apiUrl);
			final String apiResponse = IOUtils.toString(urlObj, Charset.forName("UTF-8"));
			final JSONObject json = new JSONObject(apiResponse);
			payload = Optional.ofNullable(json.getJSONObject("payload"));
			payload.ifPresent(obj -> {
				onlineInventory = Optional.ofNullable(getJsonObj(obj, "onlineInventory"));
				onlinePricing = Optional.ofNullable(getJsonObj(obj, "onlinePricing"));
				skuOptionsArr = Optional.ofNullable(getJsonArr(obj, "skuOptions"));
				if(skuOptionsArr.isPresent() && skuOptionsArr.get().length() > 0) {
					skuOptions = Optional.ofNullable(skuOptionsArr.get().getJSONObject(0));
				}
			});
			return true;
		} catch(final MalformedURLException e) {
			System.out.println("Malformed URL: " + apiUrl);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean passesAllListingConditions() {
		try {
			return isFreeShipping() && isAvailableOnline()
					&& !hasMinPurchaseQty() && !hasVariations()
					&& !isGiftCard();
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean isFreeShipping() {
		if(skuOptions.isPresent()) {
			final String shippingType = getString(skuOptions.get(), "shippingCostType");
			if(shippingType != null) {
				switch(shippingType.toLowerCase()) {
					case "freeeligible":
					case "free":
					case "included":
						return true;
					case "paid":
						//TODO FLAG LISTING FOR PURGE EXAMINATION?
						return false;
					default:
						DBLogging.high(SamsProductAPI.class, "Unknown shipping type encountered for product id " + productId + ": " + shippingType, new RuntimeException());
				}
			}
		}
		
		return false;
	}
	
	public boolean isAvailableOnline() {
		return getAvailableToSellQuantity().orElse(0) > 0;
	}
	
	public boolean hasMinPurchaseQty() {
		if(onlineInventory.isPresent()) {
			final int minPurchaseQty = getInt(onlineInventory.get(), "minPurchaseQuantity");
			return minPurchaseQty > 1;
		}
		
		return false;
	}
	
	public boolean hasVariations() {
		if(skuOptionsArr.isPresent()) {
			return skuOptionsArr.get().length() > 1;
		}
		
		return false;
	}
	
	public boolean isGiftCard() {
		if(payload.isPresent()) {
			return getBoolean(payload.get(), "giftCardTemplateProduct");
		}
		
		return false;
	}
	
	private void reset() {
		payload = Optional.empty();
		onlineInventory = Optional.empty();
		onlinePricing = Optional.empty();
		skuOptions = Optional.empty();
		productId = null;
	}
	
	public Optional<Integer> getAvailableToSellQuantity() {
		if(onlineInventory.isPresent()) {
			return Optional.ofNullable(onlineInventory.get().getInt("availableToSellQuantity"));
		}
		
		return Optional.empty();
	}
	
	public Optional<Double> getFinalPrice() {
		if(onlinePricing.isPresent()) {
			final JSONObject finalPriceObj = getJsonObj(onlinePricing.get(), "finalPrice");
			if(finalPriceObj != null) {
				return Optional.ofNullable(finalPriceObj.getDouble("currencyAmount"));
			}
		}
		
		return Optional.empty();
	}
	
	public Optional<Double> getListPrice() {
		if(onlinePricing.isPresent()) {
			final JSONObject finalPriceObj = getJsonObj(onlinePricing.get(), "listPrice");
			if(finalPriceObj != null) {
				return Optional.ofNullable(finalPriceObj.getDouble("currencyAmount"));
			}
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getProductName() {
		if(payload.isPresent()) {
			return Optional.ofNullable(getString(payload.get(), "productName"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getDescription() {
		if(payload.isPresent()) {
			return Optional.ofNullable(getString(payload.get(), "longDescription"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getBrandName() {
		if(payload.isPresent()) {
			return Optional.ofNullable(getString(payload.get(), "brandName"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getModelNumber() {
		if(payload.isPresent()) {
			return Optional.ofNullable(getString(payload.get(), "modelNumber"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getImageId() {
		if(skuOptions.isPresent()) {
			return Optional.ofNullable(getString(skuOptions.get(), "imageId"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getItemNumber() {
		if(skuOptions.isPresent()) {
			return Optional.ofNullable(getString(skuOptions.get(), "itemNumber"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getUpcCode() {
		if(skuOptions.isPresent()) {
			return Optional.ofNullable(getString(skuOptions.get(), "upc"));
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getUpcOrImageId() {
		final Optional<String> upc = getUpcCode();
		if(upc.isPresent()) {
			return upc;
		}
		
		return getImageId();
	}
	
	public Optional<String> getUPC() {
		if(skuOptions.isPresent()) {
			final String listedUPC = getUpcOrImageId().orElse(null);
			if(listedUPC != null) {
				final long parsed = Long.parseLong(listedUPC);
				final String checkDigit = Long.toString(UPCUtils.getCheckDigit(parsed));
				if(listedUPC.length() == 12) {
					//we calculate the check digit on the first 11 of this - Slave Club API is known to
					//include EANs without the check digit in the UPC field as well.
					final String upcCheckDigit = Long.toString(UPCUtils.getCheckDigit(Long.parseLong(listedUPC.substring(0, listedUPC.length() - 1))));
					if(upcCheckDigit.equals(listedUPC.substring(11, 12))) {
						//valid UPC
						return Optional.of(listedUPC);
					}
				} else if(listedUPC.length() == 11) {
					return Optional.of(listedUPC + checkDigit);
				} else if(listedUPC.length() < 11) {
					final String zeroPadding = getZeroPaddingString(11 - listedUPC.length());
					return Optional.of(zeroPadding + listedUPC + checkDigit);
				}
			}
		}
		
		return Optional.empty();
	}
	
	public Optional<String> getEAN() {
		if(skuOptions.isPresent()) {
			final String listedUPC = getUpcOrImageId().orElse(null);
			final String itemNumber = getItemNumber().orElse(null);
			if(listedUPC != null && itemNumber != null && !listedUPC.contains(itemNumber)) {
				final long parsed = Long.parseLong(listedUPC);
				final String checkDigit = Long.toString(UPCUtils.getCheckDigit(parsed));
				if(listedUPC.length() == 12) {
					return Optional.of(listedUPC + checkDigit);
				} else if(listedUPC.length() == 13) {
					if(listedUPC.startsWith("000") && !listedUPC.startsWith("0004")) { //garbage slaves club logic w/ image id
						return Optional.of(listedUPC.substring(1, 13) + checkDigit);
					}
				}
			}
		}
		
		return Optional.empty();
	}
	
	public List<ListingImage> getImages() {
		final List<ListingImage> images = new ArrayList<>();
		
		if(skuOptions.isPresent()) {		
			String imageUrl = getString(skuOptions.get(), "listImage");
			if(imageUrl.startsWith("//")) {
				imageUrl = imageUrl.substring(2);
			}
			
			if(!imageUrl.startsWith("http://")) {
				imageUrl = "http://" + imageUrl;
			}
			
			BufferedImage currentImage = ImageUtils.getImageFromUrl(imageUrl);
			double imgDifferencePercent = 0.0;
			char currentSuffix = imageUrl.charAt(imageUrl.length() - 1);
			
			while((imgDifferencePercent = ImageUtils.getDifferencePercent(currentImage, notFoundImage)) > 0) {
				//System.out.println("currentSuffix: " + currentSuffix + ", imgDifferencePercent = " + imgDifferencePercent);
				final String adjustedResolutionImageUrl = imageUrl.contains("?$DT_Zoom$") ? imageUrl : imageUrl + "?$DT_Zoom$";
				images.add(new ListingImage(adjustedResolutionImageUrl));
				
				currentSuffix = (char)(((int)currentSuffix) + 1);
				imageUrl = imageUrl.substring(0, imageUrl.length() - 1) + currentSuffix;
				currentImage = ImageUtils.getImageFromUrl(imageUrl);
			}
		}
		
		return images;
	}
	
	private String getZeroPaddingString(final int length) {
		String str = "";
		
		for(int i = 0; i < length; i++) {
			str += "0";
		}
		
		return str;
	}
}
