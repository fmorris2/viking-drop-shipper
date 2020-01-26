package main.org.vikingsoftware.dropshipper.core.ebay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;

public class EbayApiContextManager {
	
	private static final String CREDS_FILE = "/data/ebay-tokens.secure";
	private static final String SANDBOX_API_URL = "https://api.sandbox.ebay.com/wsapi";
	private static final String LIVE_API_URL = "https://api.ebay.com/wsapi";
	
	private static ApiContext sandboxBuyerContext;
	private static ApiContext sandboxSellerContext;
	private static ApiContext liveContext;
	
	private static String sandboxBuyerToken;
	private static String sandboxSellerToken;
	private static String liveUserToken;
	
	static {
		parseCreds();
	}
	
	private EbayApiContextManager() {
		
	}
	
	public static ApiContext getSandboxBuyerContext() {
		if(sandboxBuyerContext == null) {
			sandboxBuyerContext = createApiContext(SANDBOX_API_URL, sandboxBuyerToken);
		}
		
		return sandboxBuyerContext;
	}
	
	public static ApiContext getSandboxSellerContext() {
		if(sandboxSellerContext == null) {
			sandboxSellerContext = createApiContext(SANDBOX_API_URL, sandboxSellerToken);
		}
		
		return sandboxSellerContext;
	}
	
	public static ApiContext getLiveContext() {
		if(liveContext == null) {
			liveContext = createApiContext(LIVE_API_URL, liveUserToken);
		}
		
		return liveContext;
	}
	
	private static ApiContext createApiContext(final String apiEndpoint, final String userToken) {
		final ApiContext apiContext = new ApiContext();
		apiContext.setApiServerUrl(apiEndpoint);
		
		final ApiCredential cred = apiContext.getApiCredential();
		cred.seteBayToken(userToken);
		
		return apiContext;
	}
	
	private static void parseCreds() {
		try(
			final InputStream inputStream = EbayApiContextManager.class.getResourceAsStream(CREDS_FILE);
			final InputStreamReader reader = new InputStreamReader(inputStream);
			final BufferedReader bR = new BufferedReader(reader);
		) {
			sandboxBuyerToken = bR.readLine().trim();
			sandboxSellerToken = bR.readLine().trim();
			liveUserToken = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
