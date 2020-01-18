package main.org.vikingsoftware.dropshipper.core.net.http;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
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
