package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SamsClubUtils {
	
	private static final String[] orderIdPatterns = {
		"orderDetailsPage.jsp?orderId=(\\d*)&amp;",
		"orderId=(\\d*)\"></a>",
		"'purchaseID' : '(\\d*)',",
		"\"order_id\":\"(\\d*)\",",
		";ord=(\\d*);gtm=",
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
