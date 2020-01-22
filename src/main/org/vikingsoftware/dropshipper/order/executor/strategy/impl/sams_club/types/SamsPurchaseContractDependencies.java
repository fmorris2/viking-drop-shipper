package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.org.vikingsoftware.dropshipper.core.data.misc.Pair;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsRequest;


public class SamsPurchaseContractDependencies {
	
	private static final String PURCHASE_CONTRACT_DEPENDENCIES_URL = "https://www.samsclub.com/sams/cart/cart.jsp";
	
	private static final String SOA_MAPPINGS_REGEX = "var soaHeaders\\s?=\\s?(\\{[^\\};]+\\})";
	private static final String CXO_MAPPINGS_REGEX = "var cxoHeaders\\s?=\\s?(\\{[^\\};]+\\})";
	
	private static final Pattern SOA_MAPPINGS_PATTERN = Pattern.compile(SOA_MAPPINGS_REGEX);
	private static final Pattern CXO_MAPPINGS_PATTERN = Pattern.compile(CXO_MAPPINGS_REGEX);
	
	public final SamsClubAddress address;
	
	private final Map<String, String> mappings;
	private final Map<String, String> session;
	private final WrappedHttpClient client;
	
	private CookieStore cookies;
	
	public SamsPurchaseContractDependencies(final WrappedHttpClient client, final Map<String, String> session) {
		this.client = client;
		this.session = session;
		final Pair<Document, String> data = generateHtml();
		this.mappings = generateMappingsFromHtml(data.right);
		this.address = generateAddressFromHtml(data.left);
	}
	
	public String getMapping(final String key) {
		return mappings.get(key.toLowerCase());
	}
	
	public CookieStore getCookieStore() {
		return cookies;
	}
	
	private Pair<Document, String> generateHtml() {
		try {
			/*
			 * This first part is so we can get Apache to handle the cookies
			 * from the Cart.jsp page and modify our client with them
			 */
			final HttpGet request = new HttpGet(PURCHASE_CONTRACT_DEPENDENCIES_URL);
			addHeaders(request);
			client.setCookies("samsclub.com", "/", session);
			client.execute(request);
			
			final Document doc = Jsoup.connect(PURCHASE_CONTRACT_DEPENDENCIES_URL)
					.headers(WrappedHttpClient.generateHeaderMapFromRequest(request))
					.ignoreContentType(true)
					.cookies(session)
					.get();
			
			return new Pair<>(doc, doc.html());
		} catch(final IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void addHeaders(final HttpGet request) {
		request.addHeader("user-agent", SamsRequest.DEFAULT_USER_AGENT);
		request.addHeader("authority", "www.samsclub.com");
		request.addHeader("cache-control", "max-age=0");
		request.addHeader("dnt", "1");
		request.addHeader("upgrade-insecure-requests", "1");
		request.addHeader("sec-fetch-user", "?1");
		request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
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
	
	private SamsClubAddress generateAddressFromHtml(final Document document) {
		final Elements elements = document.getElementsByClass("js-profile-shipping-address");
		if(!elements.isEmpty()) {
			final Element el = elements.first();
			return new SamsClubAddress.Builder()
					.addressId(el.attr("data-addressid"))
					.addressType(el.attr("data-addresstype"))
					.firstName(el.attr("data-firstname"))
					.lastName(el.attr("data-lastname"))
					.middleName(el.attr("data-middlename"))
					.addressLineOne(el.attr("data-addresslineone"))
					.city(el.attr("data-city"))
					.prefix(el.attr("data-prefix"))
					.stateOrProvinceCode(el.attr("data-stateorprovincecode"))
					.postalCode(el.attr("data-postalcode"))
					.countryCode(el.attr("data-countrycode"))
					.phone(el.attr("data-phone"))
					.isDefault(Boolean.parseBoolean(el.attr("data-isdefault")))
					.suffix(el.attr("data-suffix"))
					.businessName(el.attr("data-businessname"))
					.phoneNumberType(el.attr("data-phonenumbertype"))
					.phoneTwoType(el.attr("data-phonetwotype"))
					.phoneTwo(el.attr("data-phonetwo"))
					.nickName(el.attr("data-nickname"))
					.addressLineTwo(el.attr("data-addresslinetwo"))
					.addressLineThree(el.attr("data-addresslinethree"))
					.dockDoorPresent(el.attr("data-dockdoorpresent"))
					.build();
		} else {
			System.err.println("Failed to generate address from HTML!");
		}
		
		return null;
	}
	
	private void populateMappings(final Map<String, String> mappings, final JSONObject obj) {
		System.out.println("populateMappings from JSONObject: " + obj);
		for(final String key : obj.keySet()) {
			System.out.println("Populating Purchase Contract Dependency: " + key + ":" + obj.get(key));
			mappings.put(key.toLowerCase(), obj.get(key).toString());
		}
	}
	
}
