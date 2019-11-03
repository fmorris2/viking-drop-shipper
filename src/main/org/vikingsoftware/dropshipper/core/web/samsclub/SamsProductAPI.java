package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.utils.UPCUtils;

public final class SamsProductAPI {
	
	private static final String API_BASE_URL = "https://www.samsclub.com/api/soa/services/v1/catalog/product/";
	private static final String API_URL_ARGS = "?response_group=LARGE&clubId=6279";
	
	private Optional<JSONObject> payload;
	private Optional<JSONObject> onlineInventory;
	private Optional<JSONObject> onlinePricing;
	private Optional<JSONObject> skuOptions;
	
	public static void main(final String[] args) {
		final SamsProductAPI api = new SamsProductAPI();
		api.parse("prod21220949");
		System.out.println("Current Stock: " + api.getAvailableToSellQuantity().orElse(0));
		System.out.println("Final Price: " + api.getFinalPrice().orElse(0D));
		System.out.println("UPC: " + api.getUPC().orElse(null));
	}
	
	public boolean parse(final String productId) {
		try {
			reset();
			final String apiUrl = API_BASE_URL + productId + API_URL_ARGS;
			final URL urlObj = new URL(apiUrl);
			final String apiResponse = IOUtils.toString(urlObj, Charset.forName("UTF-8"));
			final JSONObject json = new JSONObject(apiResponse);
			payload = Optional.ofNullable(json.getJSONObject("payload"));
			payload.ifPresent(obj -> {
				onlineInventory = Optional.ofNullable(getJsonObj(obj, "onlineInventory"));
				onlinePricing = Optional.ofNullable(getJsonObj(obj, "onlinePricing"));
				final JSONArray skuOptionsArr = getJsonArr(obj, "skuOptions");
				if(skuOptionsArr != null && skuOptionsArr.length() > 0) {
					skuOptions = Optional.ofNullable(skuOptionsArr.getJSONObject(0));
				}
			});
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private JSONObject getJsonObj(final JSONObject parent, final String obj) {
		try {
			return parent.getJSONObject(obj);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	private JSONArray getJsonArr(final JSONObject parent, final String obj) {
		try {
			return parent.getJSONArray(obj);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	private String getString(final JSONObject parent, final String key) {
		try {
			return parent.getString(key);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	private void reset() {
		payload = Optional.empty();
		onlineInventory = Optional.empty();
		onlinePricing = Optional.empty();
		skuOptions = Optional.empty();
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
	
	public Optional<String> getProductName() {
		if(payload.isPresent()) {
			return Optional.ofNullable(getString(payload.get(), "productName"));
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
	
	private String getZeroPaddingString(final int length) {
		String str = "";
		
		for(int i = 0; i < length; i++) {
			str += "0";
		}
		
		return str;
	}
}
