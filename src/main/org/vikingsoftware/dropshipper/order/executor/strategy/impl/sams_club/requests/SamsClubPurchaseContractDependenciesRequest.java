package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types.SamsClubResponse;


public class SamsClubPurchaseContractDependenciesRequest extends SamsClubRequest {
	
	private static final String PURCHASE_CONTRACT_DEPENDENCIES_URL = "https://www.samsclub.com/sams/cart/cart.jsp";
	
	private static final String SOA_MAPPINGS_REGEX = "var soaHeaders\\s?=\\s?(\\{[^\\};]+\\})";
	private static final String CXO_MAPPINGS_REGEX = "var cxoHeaders\\s?=\\s?(\\{[^\\};]+\\})";
	
	private static final Pattern SOA_MAPPINGS_PATTERN = Pattern.compile(SOA_MAPPINGS_REGEX);
	private static final Pattern CXO_MAPPINGS_PATTERN = Pattern.compile(CXO_MAPPINGS_REGEX);
	
	private Map<String, String> mappings;
	
	private CookieStore cookies;
	
	public SamsClubPurchaseContractDependenciesRequest(final WrappedHttpClient client) {
		super(client);
	}
	
	public String getMapping(final String key) {
		return mappings.get(key.toLowerCase());
	}
	
	public CookieStore getCookieStore() {
		return cookies;
	}
	
	public boolean execute() {
		final Optional<SamsClubResponse> data = generateHtml();
		if(data.isEmpty() || !data.get().success) {
			return false;
		}
		
		this.mappings = generateMappingsFromHtml(data.get().response);
		return true;
	}
	
	private Optional<SamsClubResponse> generateHtml() {
		/*
		 * This first part is so we can get Apache to handle the cookies
		 * from the Cart.jsp page and modify our client with them
		 */
		final HttpGet request = new HttpGet(PURCHASE_CONTRACT_DEPENDENCIES_URL);
		addHeaders(request);
		final Optional<SamsClubResponse> response = sendRequest(client, request, HttpStatus.SC_OK);
		
		return response;
	}
	
	private void addHeaders(final HttpGet request) {
		request.addHeader("user-agent", SamsClubRequest.DEFAULT_USER_AGENT);
		request.addHeader("authority", "www.samsclub.com");
		request.addHeader("cache-control", "max-age=0");
		request.addHeader("dnt", "1");
		request.addHeader("upgrade-insecure-requests", "1");
		request.addHeader("sec-fetch-user", "?1");
		request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		request.addHeader("sec-fetch-site", "same-origin");
		request.addHeader("sec-fetch-mode", "navigate");
		request.addHeader("accept-encoding", "gzip, deflate, br");
		request.addHeader("accept-language", "en-US,en;q=0.9,pt;q=0.8");
	}
	 
	private Map<String,String> generateMappingsFromHtml(final String html) {
		final Map<String,String> mappings = new HashMap<>();
		
		final Matcher soaMatcher = SOA_MAPPINGS_PATTERN.matcher(html);
		final Matcher cxoMatcher = CXO_MAPPINGS_PATTERN.matcher(html);
		
		if(soaMatcher.find()) {
			System.out.println("SOA Matcher has found a match: " + soaMatcher.group(1));
			populateMappings(mappings, new JSONObject(soaMatcher.group(1)));
		}
		
		if(cxoMatcher.find()) {
			System.out.println("CXO Matcher has found a match: " + soaMatcher.group(1));
			populateMappings(mappings, new JSONObject(cxoMatcher.group(1)));
		}
		return mappings;
	}
	
	private void populateMappings(final Map<String, String> mappings, final JSONObject obj) {
		System.out.println("populateMappings from JSONObject: " + obj);
		for(final String key : obj.keySet()) {
			System.out.println("Populating Purchase Contract Dependency: " + key + ":" + obj.get(key));
			mappings.put(key.toLowerCase(), obj.get(key).toString());
		}
	}
	
}
