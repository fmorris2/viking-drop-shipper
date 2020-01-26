package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.Map;

import org.json.JSONObject;

public class SamsClubLoginResponse {
	public final Map<String, String> cookies;
	public final JSONObject json;
	
	public SamsClubLoginResponse(final Map<String, String> cookies, final JSONObject json) {
		this.cookies = cookies;
		this.json = json;
	}
}
