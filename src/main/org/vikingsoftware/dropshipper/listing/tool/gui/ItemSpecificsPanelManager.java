package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.util.Map;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public final class ItemSpecificsPanelManager {

	private static ItemSpecificsPanelManager instance;
	
	private ItemSpecificsPanelManager() {
		//singleton
	}
	
	public static synchronized ItemSpecificsPanelManager get() {
		if(instance == null) {
			instance = new ItemSpecificsPanelManager();
		}
		
		return instance;
	}

	public Map<String, String> getRequiredItemSpecifics(final Listing listing) {
		return null;
	}
	
	
}
