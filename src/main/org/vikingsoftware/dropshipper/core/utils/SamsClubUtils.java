package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SamsClubUtils {
	
	private static final String[] orderIdPatterns = {
		"value=\"orderId=(\\d*)&",
		".returnOrderId\" value=\"(\\d+)\"",
		"Order Number:&nbsp;(\\d+)</h3>"
	};
	
	private SamsClubUtils() {
		//can't instantiate util classes
	}
	
	public static String getOrderNumberFromPageSource(final String pageSource) {
		for(final String pattern : orderIdPatterns) {
			final Matcher matcher = Pattern.compile(pattern).matcher(pageSource);
			if(matcher.find()) {
				return matcher.group(1);
			}
		}
		
		return null;
	}

}
