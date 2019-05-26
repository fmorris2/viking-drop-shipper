package test.org.vikingsoftware.dropshipper.http;

import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class TestCostcoLogin {

	@Test
	public void test() {
		try {
			final RequestConfig globalConfig = RequestConfig.custom()
					.setCookieSpec(CookieSpecs.DEFAULT)
					.build();

			final CookieStore cookieStore = new BasicCookieStore();
		    final HttpClientContext context = HttpClientContext.create();
		    context.setCookieStore(cookieStore);

		    final CloseableHttpClient httpClient = HttpClients.custom()
		    		.setDefaultRequestConfig(globalConfig)
		    		.setDefaultCookieStore(cookieStore)
		    		.build();

		    Unirest.config().reset();
		    Unirest.config().httpClient(httpClient);
			Unirest.config().setDefaultHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
			final HttpResponse<String> initialCookieResponse = Unirest.get("https://www.costco.com/logonform")
					.header("authority", "www.costco.com")
					.header("method", "GET")
					.header("path", "/logonform")
					.header("scheme", "https")
					.header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
					.header("accept-encoding", "gzip, deflate, br")
					.header("accept-language", "en-US,en;q=0.9")
					.header("cache-control", "no-cache")
					.header("pragma", "no-cache")
					.header("upgrade-insecure-requests", "1")
					.header("content-type", "text/plain;charset=UTF-8")
					.header("origin", "https://www.costco.com")
					.asString();

			final String body = initialCookieResponse.getBody();
			final Document doc = Jsoup.parse(body);
			final String authToken = doc.selectFirst("input[name=\"authToken\"]").attr("value");

			System.out.println("GET Completed");
			System.out.println("\tCookies: " + getCookieString(cookieStore));

			final HttpResponse<String> abckCookieReq = Unirest.post("https://www.costco.com/static/5efb7b381d51455a1cca1b93625b35e")
					.header("accept", "*/*")
					.header("accept-encoding", "gzip, deflate, br")
					.header("accept-language", "en-US,en;q=0.9")
					.header("cache-control", "no-cache")
					.header("content-type", "text/plain;charset=UTF-8")
					.header("Cookie", getCookieString(cookieStore))
					.header("origin", "https://www.costco.com")
					.header("pragma", "no-cache")
					.header("dnt", "1")
					.header("referer", "https://www.costco.com/LogonForm")
					.header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36")
					.body("{\"sensor_data\":\"7a74G7m23Vrp0o5c9079361.41-1,2,-94,-100,Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36,uaend,12147,20030107,en-US,Gecko,3,0,0,0,383291,6099652,1618,1050,1680,1050,582,947,1618,,cpen:0,i1:0,dm:0,cwen:0,non:1,opc:0,fc:0,sc:0,wrc:1,isc:0,vib:1,bat:1,x11:0,x12:1,8326,0.557541358278,778898049824.5,loc:-1,2,-94,-101,do_en,dm_en,t_en-1,2,-94,-105,0,-1,0,0,3022,857,0;0,-1,0,0,2318,1118,0;0,-1,0,0,1191,773,0;0,-1,0,0,2896,857,0;0,-1,0,0,716,716,0;1,0,0,0,1394,1394,0;0,-1,0,0,1891,857,0;0,-1,0,0,1910,1118,0;-1,2,-94,-102,0,-1,0,0,3022,857,0;0,-1,0,0,2318,1118,0;0,-1,0,0,1191,773,0;0,-1,0,0,2896,857,0;0,-1,0,0,716,716,0;1,0,0,0,1394,1394,0;0,-1,0,0,1891,857,0;0,-1,0,0,1910,1118,0;-1,2,-94,-108,-1,2,-94,-110,-1,2,-94,-117,-1,2,-94,-111,-1,2,-94,-109,-1,2,-94,-114,-1,2,-94,-103,-1,2,-94,-112,https://www.costco.com/LogonForm-1,2,-94,-115,1,1,0,0,0,0,0,8,0,1557796099649,-999999,16664,0,0,2777,0,0,16,0,0,982A567DFE561C1605DA45522BFBA308172B39C5B11100000615DA5C4B2B3215~-1~MZhjV0Ka9WnJRBQN/LI5pjusU5XQbD8E9P5NUyKKSy0=~-1~-1,7922,-1,-1,30261693-1,2,-94,-106,0,0-1,2,-94,-119,-1-1,2,-94,-122,0,0,0,0,1,0,0-1,2,-94,-123,-1,2,-94,-70,-1-1,2,-94,-80,94-1,2,-94,-116,6099668-1,2,-94,-118,66771-1,2,-94,-121,;12;-1;0\"}")
					.asString();

			System.out.println("abck POST Completed");

			System.out.println("abck POST response: " + abckCookieReq.getBody());

			final HttpResponse<String> loginPOST = Unirest.post("https://www.costco.com/Logon")
					  .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
					  .header("accept-encoding", "gzip, deflate, br")
					  .header("accept-language", "en-US,en;q=0.9")
					  .header("pragma", "no-cache")
					  .header("content-type", "application/x-www-form-urlencoded")
					  .header("origin", "https://www.costco.com")
					  .header("referer", "https://www.costco.com/LogonForm")
					  .header("cache-control", "no-cache")
					  .header("upgrade-insecure-requests", "1")
					  .header("Cookie", getCookieString(cookieStore))
					  .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36")
					  .field("logonId", "thevikingmarketplace@gmail.com")
					  .field("logonPassword", "dO8h3rP7SDc932S")
					  .field("reLogonURL", "LogonForm")
					  .field("fromCheckout", "")
					  .field("authToken", authToken)
					  .field("isPharmacy", "false")
					  .field("URL", "Lw==")
					  .asString();

			System.out.println("POST Completed");
			System.out.println("Response: " + loginPOST.getBody());

		} catch (final UnirestException e) {
			e.printStackTrace();
		}
	}

	private String getCookieString(final CookieStore cookieStore) {
		final StringBuilder builder = new StringBuilder();
		for(final Cookie cookie : cookieStore.getCookies()) {
			builder.append(cookie.getName());
			builder.append("=");
			builder.append(cookie.getValue());
			builder.append("; ");
		}

		return builder.toString();
	}

}
