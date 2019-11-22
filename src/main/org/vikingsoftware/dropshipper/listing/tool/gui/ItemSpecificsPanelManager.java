package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics.ItemSpecificsPanel;
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
		final AtomicBoolean finished = new AtomicBoolean(false);
		
		SwingUtilities.invokeLater(() -> {
			new ItemSpecificsPanel(listing, finished);
		});
		
		while(!finished.get()) {
			try {
				Thread.sleep(10);
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
		
		return listing.itemSpecifics;
	}
	
	
}
