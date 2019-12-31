package main.org.vikingsoftware.dropshipper.core.web;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonAPIParser {
	
	protected JSONObject getJsonObj(final JSONObject parent, final String obj) {
		try {
			return parent.getJSONObject(obj);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	protected JSONArray getJsonArr(final JSONObject parent, final String obj) {
		try {
			return parent.getJSONArray(obj);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	protected String getString(final JSONObject parent, final String key) {
		try {
			return parent.getString(key);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return null;
	}
	
	protected int getInt(final JSONObject parent, final String key) {
		try {
			return parent.getInt(key);
		} catch(final Exception e) {
			//swallow exception
		}
		
		return -1;
	}
}
