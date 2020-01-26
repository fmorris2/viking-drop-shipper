package main.org.vikingsoftware.dropshipper.core.web;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonAPIParser {
	
	public static JSONObject getJsonObj(final JSONObject parent, final String obj) {
		try {
			return parent.getJSONObject(obj);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	public static JSONArray getJsonArr(final JSONObject parent, final String obj) {
		try {
			return parent.getJSONArray(obj);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	public static String getString(final JSONObject parent, final String key) {
		try {
			return parent.getString(key);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	public static int getInt(final JSONObject parent, final String key) {
		try {
			return parent.getInt(key);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return -1;
	}
	
	public static boolean getBoolean(final JSONObject parent, final String key) {
		try {
			return parent.getBoolean(key);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return false;
	}
}
