package main.org.vikingsoftware.dropshipper.crawler.strategy.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import main.org.vikingsoftware.dropshipper.core.net.ConnectionManager;
import main.org.vikingsoftware.dropshipper.core.web.JsonAPIParser;

public class SamsCategoryAPI extends JsonAPIParser {
	
	private static final int PRODUCTS_PER_CALL = 48; //max you can request at once
	private static final String API_BASE_URL = "http://www.samsclub.com/api/node/vivaldi/v1/products/search/?sourceType=1&sortKey=p_sales_rank&sortOrder=1"
			+ "&clubId=6279&limit="+PRODUCTS_PER_CALL+"&br=true&searchCategoryId=";
	
	private String categoryId;
	
	private Optional<JSONObject> payload;
	private Optional<JSONArray> records;
	
	public static void main(final String[] args) {
		final SamsCategoryAPI api = new SamsCategoryAPI();
		api.parse("1959");
		System.out.println("Parsed " + api.getProductUrls().size() + " products");
	}
	
	public boolean parse(final String categoryId) {
		return parse(categoryId, 0);
	}
	
	private boolean parse(final String categoryId, final int offset) {
		this.categoryId = categoryId;
		String apiUrl = null;
		try {
			reset();
			apiUrl = API_BASE_URL + categoryId + "&offset=" + offset;
			final Document doc = ConnectionManager.get().getConnection()
					  .url(apiUrl)
				      .ignoreContentType(true)
				      .get();
			final JSONObject json = new JSONObject(doc.text());
			payload = Optional.ofNullable(json.getJSONObject("payload"));
			payload.ifPresent(obj -> {
				records = Optional.ofNullable(getJsonArr(obj, "records"));
			});
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public Set<String> getProductUrls() {
		Set<String> urls = new HashSet<>();
		final int totalRecords = getInt(payload.get(), "totalRecords");
		int currentOffset = 0;
		while(currentOffset < totalRecords) {
			final SamsCategoryAPI api = new SamsCategoryAPI();
			api.parse(categoryId, currentOffset);
			if(api.records.isPresent()) {
				for(int i = 0; i < api.records.get().length(); i++) {
					final JSONObject productJsonObj = (JSONObject)api.records.get().get(i);
					urls.add("https://www.samsclub.com"+getString(productJsonObj, "seoUrl"));
				}
			}
			currentOffset += PRODUCTS_PER_CALL;
		}
		
		return urls;
	}
	
	public void reset() {
		payload = Optional.empty();
		records = Optional.empty();
	}
}
