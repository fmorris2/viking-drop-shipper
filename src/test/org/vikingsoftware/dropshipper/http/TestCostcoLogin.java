package test.org.vikingsoftware.dropshipper.http;

import org.junit.Test;

import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;

public class TestCostcoLogin {

	@Test
	public void test() {
		try {
			Unirest.setDefaultHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");
			final HttpResponse<String> initialCookieResponse = Unirest.get("https://www.costco.com/LogonForm")
					.header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
					.header("accept-encoding", "gzip, deflate, br")
					.header("accept-language", "en-US,en;q=0.9")
					.header("cache-control", "no-cache")
					.header("content-type", "text/plain;charset=UTF-8")
					.header("origin", "https://www.costco.com")
					.header("pragma", "no-cache")
					.asString();
			final Headers initialResponseHeaders = initialCookieResponse.getHeaders();
			System.out.println("initialResponseHeaders: " + initialResponseHeaders);
			String cookieString = "";
			for(final String cookie : initialResponseHeaders.get("Set-Cookie")) {
				cookieString += cookie + ";";
			}
			System.out.println("initialResponseHeaders: " + initialResponseHeaders);
			//System.out.println("About to send request...");
			final MultipartBody request = Unirest.post("https://www.costco.com/Logon")
					  .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
					  .header("content-type", "application/x-www-form-urlencoded")
					  .header("origin", "https://www.costco.com")
					  .header("referer", "https://www.costco.com/LogonForm")
					  .header("cache-control", "no-cache")
					  .header("upgrade-insecure-requests", "1")
					  .header("cookie", cookieString)
					  .field("logonId", "thevikingmarketplace@gmail.com")
					  .field("logonPassword", "dO8h3rP7SDc932S")
					  .field("reLogonURL", "LogonForm")
					  .field("isPharmacy", "false")
					  .field("URL", "Lw==");

			final HttpResponse<String> response = request.asString();
			System.out.println("Request headers: " + request.getHttpRequest().getHeaders());
			System.out.println("Response: " + response.getBody());
		} catch (final UnirestException e) {
			e.printStackTrace();
		}
	}

}
