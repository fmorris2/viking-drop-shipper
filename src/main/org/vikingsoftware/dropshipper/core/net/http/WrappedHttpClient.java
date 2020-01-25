package main.org.vikingsoftware.dropshipper.core.net.http;

import java.io.IOException;
import java.net.SocketException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.net.proxy.ProxyAuthenticationCooldownException;
import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxy;

public class WrappedHttpClient {
	
	public final HttpClient client;
	public final VSDSProxy proxy;
	public final HttpClientContext context;
	
	public WrappedHttpClient(final HttpClient client, final VSDSProxy proxy) {
		this.client = client;
		this.proxy = proxy;
		this.context = HttpClientContext.create();
		this.context.setCookieStore(new BasicCookieStore());
	}
	
	public HttpResponse execute(final HttpUriRequest request) throws ClientProtocolException, IOException {
		try {
			if(proxy != null && proxy.supportsSocks() && context.getAttribute("socks.address") == null) {
				this.context.setAttribute("socks.address", proxy.generateSocksAddress());
			}
			return client.execute(request, context);
		} catch(final IndexOutOfBoundsException e) { //happens on too many redirects?
			HttpClientManager.get().flag(this);
		} catch(final SocketException e) {
			if(proxy != null && e.getMessage().contains("authentication failed")) {
				System.out.println("Failed to authenticate proxy: " + this);
				HttpClientManager.get().flag(this);
				HttpClientManager.get().reportFailedProxyConnectionAttempt(proxy.source);
				throw new ProxyAuthenticationCooldownException(e);
			} else {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public CookieStore getCookieStore() {
		return context.getCookieStore();
	}
	
	public void clearCookies() {
		context.getCookieStore().clear();
	}
	
	public void setCookies(final String domain, final String path, final Map<String, String> cookieMap) {
		for(final Map.Entry<String, String> cookieEntry : cookieMap.entrySet()) {
			final BasicClientCookie cookie = new BasicClientCookie(cookieEntry.getKey(), cookieEntry.getValue());
			cookie.setDomain(domain);
			cookie.setPath(path);
			cookie.setAttribute(ClientCookie.PATH_ATTR, path);
			cookie.setAttribute(ClientCookie.DOMAIN_ATTR, domain);
			context.getCookieStore().addCookie(cookie);
		}
	}
	
	public static Map<String, String> generateHeaderMapFromRequest(final HttpRequestBase post) {
		final Map<String, String> headers = new HashMap<>();
		
		for(final Header header : post.getAllHeaders()) {
			headers.put(header.getName(), header.getValue());
		}
		
		return headers;
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
