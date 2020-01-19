package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;


public class SamsPurchaseContractDependencies {
	
	private static final String SOA_MAPPINGS_REGEX = "var soaHeaders\\s?=\\s?(\\{[^\\};]+\\})";
	private static final String CXO_MAPPINGS_REGEX = "var cxoHeaders\\s?=\\s?(\\{[^\\};]+\\})";
	
	private static final Pattern SOA_MAPPINGS_PATTERN = Pattern.compile(SOA_MAPPINGS_REGEX);
	private static final Pattern CXO_MAPPINGS_PATTERN = Pattern.compile(CXO_MAPPINGS_REGEX);
	
	private final Map<String, String> mappings;
	
	public SamsPurchaseContractDependencies(final String html) {
		mappings = generateMappingsFromHtml(html);
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
		
		System.out.println("[SamsPurchaseContractDependencies] Attempting to parse mappings from html: " + html);
		
		return mappings;
	}
	
	private void populateMappings(final Map<String, String> mappings, final JSONObject obj) {
		System.out.println("populateMappings from JSONObject: " + obj);
		for(final String key : obj.keySet()) {
			System.out.println("Populating Purchase Contract Dependency: " + key + ":" + obj.get(key));
			mappings.put(key, obj.get(key).toString());
		}
	}
	
}
