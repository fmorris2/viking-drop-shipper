package main.org.vikingsoftware.dropshipper.core.net.http;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;

public class WrappedHttpClient {
	
	public final HttpClient client;
	public final VSDSProxy proxy;
	
	public WrappedHttpClient(final HttpClient client, final VSDSProxy proxy) {
		this.client = client;
		this.proxy = proxy;
	}
	
	public HttpResponse execute(final HttpUriRequest request) throws ClientProtocolException, IOException {
		return client.execute(request);
	}
	
	public HttpResponse execute(final HttpUriRequest request, final HttpContext context) throws ClientProtocolException, IOException {
		return client.execute(request, context);
	}
	
	public HttpContext createContextFromCookies(final CookieStore cookies) {
		final HttpContext localContext = new HttpClientContext();
		localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookies);
		return localContext;
	}
	
	public HttpContext createContextFromCookies(final CookieStore cookieStore, final String domain, final Map<String, String> cookieMap) {
		for(final Map.Entry<String, String> cookieEntry : cookieMap.entrySet()) {
			final BasicClientCookie cookie = new BasicClientCookie(cookieEntry.getKey(), cookieEntry.getValue());
			cookie.setDomain(domain);
			cookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
			cookieStore.addCookie(cookie);
		}

		return createContextFromCookies(cookieStore);
	}
	
	public static CookieStore createCookieStoreFromMap(final Map<String, String> cookies) {
		final CookieStore cookieStore = new BasicCookieStore();
		for(final Map.Entry<String, String> cookie : cookies.entrySet()) {
			cookieStore.addCookie(new BasicClientCookie(cookie.getKey(), cookie.getValue()));
		}
		
		return cookieStore;
	}
	
	public static void main(final String[] args) {
		try(final ResultSet res = VSDSDBManager.get().createStatement().executeQuery("SELECT id,listing_title FROM marketplace_listing_clone");
			final PreparedStatement st = VSDSDBManager.get().createPreparedStatement("UPDATE marketplace_listing SET listing_title=?"
					+ " WHERE id=?")) {
			while(res.next()) {
				st.setString(1, res.getString("listing_title"));
				st.setInt(2, res.getInt("id"));
				st.execute();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
