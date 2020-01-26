package main.org.vikingsoftware.dropshipper.core.data.misc;

import java.util.HashMap;
import java.util.Map;

public class StateUtils {
	
	private static final Map<String, String> ABBREV_TO_FULL_STATE_MAP;
	private static final Map<String, String> FULL_STATE_TO_ABBREV_MAP;
	
	static {
	    ABBREV_TO_FULL_STATE_MAP = new HashMap<String, String>();
	    ABBREV_TO_FULL_STATE_MAP.put("AL", "Alabama");
	    ABBREV_TO_FULL_STATE_MAP.put("AK", "Alaska");
	    ABBREV_TO_FULL_STATE_MAP.put("AB", "Alberta");
	    ABBREV_TO_FULL_STATE_MAP.put("AZ", "Arizona");
	    ABBREV_TO_FULL_STATE_MAP.put("AR", "Arkansas");
	    ABBREV_TO_FULL_STATE_MAP.put("BC", "British Columbia");
	    ABBREV_TO_FULL_STATE_MAP.put("CA", "California");
	    ABBREV_TO_FULL_STATE_MAP.put("CO", "Colorado");
	    ABBREV_TO_FULL_STATE_MAP.put("CT", "Connecticut");
	    ABBREV_TO_FULL_STATE_MAP.put("DE", "Delaware");
	    ABBREV_TO_FULL_STATE_MAP.put("DC", "District Of Columbia");
	    ABBREV_TO_FULL_STATE_MAP.put("FL", "Florida");
	    ABBREV_TO_FULL_STATE_MAP.put("GA", "Georgia");
	    ABBREV_TO_FULL_STATE_MAP.put("GU", "Guam");
	    ABBREV_TO_FULL_STATE_MAP.put("HI", "Hawaii");
	    ABBREV_TO_FULL_STATE_MAP.put("ID", "Idaho");
	    ABBREV_TO_FULL_STATE_MAP.put("IL", "Illinois");
	    ABBREV_TO_FULL_STATE_MAP.put("IN", "Indiana");
	    ABBREV_TO_FULL_STATE_MAP.put("IA", "Iowa");
	    ABBREV_TO_FULL_STATE_MAP.put("KS", "Kansas");
	    ABBREV_TO_FULL_STATE_MAP.put("KY", "Kentucky");
	    ABBREV_TO_FULL_STATE_MAP.put("LA", "Louisiana");
	    ABBREV_TO_FULL_STATE_MAP.put("ME", "Maine");
	    ABBREV_TO_FULL_STATE_MAP.put("MB", "Manitoba");
	    ABBREV_TO_FULL_STATE_MAP.put("MD", "Maryland");
	    ABBREV_TO_FULL_STATE_MAP.put("MA", "Massachusetts");
	    ABBREV_TO_FULL_STATE_MAP.put("MI", "Michigan");
	    ABBREV_TO_FULL_STATE_MAP.put("MN", "Minnesota");
	    ABBREV_TO_FULL_STATE_MAP.put("MS", "Mississippi");
	    ABBREV_TO_FULL_STATE_MAP.put("MO", "Missouri");
	    ABBREV_TO_FULL_STATE_MAP.put("MT", "Montana");
	    ABBREV_TO_FULL_STATE_MAP.put("NE", "Nebraska");
	    ABBREV_TO_FULL_STATE_MAP.put("NV", "Nevada");
	    ABBREV_TO_FULL_STATE_MAP.put("NB", "New Brunswick");
	    ABBREV_TO_FULL_STATE_MAP.put("NH", "New Hampshire");
	    ABBREV_TO_FULL_STATE_MAP.put("NJ", "New Jersey");
	    ABBREV_TO_FULL_STATE_MAP.put("NM", "New Mexico");
	    ABBREV_TO_FULL_STATE_MAP.put("NY", "New York");
	    ABBREV_TO_FULL_STATE_MAP.put("NF", "Newfoundland");
	    ABBREV_TO_FULL_STATE_MAP.put("NC", "North Carolina");
	    ABBREV_TO_FULL_STATE_MAP.put("ND", "North Dakota");
	    ABBREV_TO_FULL_STATE_MAP.put("NT", "Northwest Territories");
	    ABBREV_TO_FULL_STATE_MAP.put("NS", "Nova Scotia");
	    ABBREV_TO_FULL_STATE_MAP.put("NU", "Nunavut");
	    ABBREV_TO_FULL_STATE_MAP.put("OH", "Ohio");
	    ABBREV_TO_FULL_STATE_MAP.put("OK", "Oklahoma");
	    ABBREV_TO_FULL_STATE_MAP.put("ON", "Ontario");
	    ABBREV_TO_FULL_STATE_MAP.put("OR", "Oregon");
	    ABBREV_TO_FULL_STATE_MAP.put("PA", "Pennsylvania");
	    ABBREV_TO_FULL_STATE_MAP.put("PE", "Prince Edward Island");
	    ABBREV_TO_FULL_STATE_MAP.put("PR", "Puerto Rico");
	    ABBREV_TO_FULL_STATE_MAP.put("QC", "Quebec");
	    ABBREV_TO_FULL_STATE_MAP.put("RI", "Rhode Island");
	    ABBREV_TO_FULL_STATE_MAP.put("SK", "Saskatchewan");
	    ABBREV_TO_FULL_STATE_MAP.put("SC", "South Carolina");
	    ABBREV_TO_FULL_STATE_MAP.put("SD", "South Dakota");
	    ABBREV_TO_FULL_STATE_MAP.put("TN", "Tennessee");
	    ABBREV_TO_FULL_STATE_MAP.put("TX", "Texas");
	    ABBREV_TO_FULL_STATE_MAP.put("UT", "Utah");
	    ABBREV_TO_FULL_STATE_MAP.put("VT", "Vermont");
	    ABBREV_TO_FULL_STATE_MAP.put("VI", "Virgin Islands");
	    ABBREV_TO_FULL_STATE_MAP.put("VA", "Virginia");
	    ABBREV_TO_FULL_STATE_MAP.put("WA", "Washington");
	    ABBREV_TO_FULL_STATE_MAP.put("WV", "West Virginia");
	    ABBREV_TO_FULL_STATE_MAP.put("WI", "Wisconsin");
	    ABBREV_TO_FULL_STATE_MAP.put("WY", "Wyoming");
	    ABBREV_TO_FULL_STATE_MAP.put("YT", "Yukon Territory");
	    
	    FULL_STATE_TO_ABBREV_MAP = generateFullStateToAbbrevMap();
	}
	
	public static String getStateNameFromCode(final String code) {
		return ABBREV_TO_FULL_STATE_MAP.get(code.toUpperCase());
	}
	
	public static String getCodeFromStateName(final String stateName) {
		return FULL_STATE_TO_ABBREV_MAP.get(stateName);
	}
	
	public static boolean isStateCode(final String code) {
		return ABBREV_TO_FULL_STATE_MAP.containsKey(code.toUpperCase());
	}
	
	public static boolean isFullStateName(final String stateName) {
		return FULL_STATE_TO_ABBREV_MAP.containsKey(stateName);
	}
	
	private static Map<String, String> generateFullStateToAbbrevMap() {
		final Map<String, String> toReturn = new HashMap<>();
		for(final Map.Entry<String, String> entry : ABBREV_TO_FULL_STATE_MAP.entrySet()) {
			toReturn.put(entry.getValue(), entry.getKey());
		}
		
		return toReturn;
	}
}
