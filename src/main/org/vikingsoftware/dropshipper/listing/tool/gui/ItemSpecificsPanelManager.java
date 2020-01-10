package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.Component;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
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

	public Map<String, String> getRequiredItemSpecifics(final ListingToolGUI gui, final Listing listing) {		
		final AtomicBoolean finished = new AtomicBoolean(false);
		
		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new ItemSpecificsPanel(listing, finished);
			frame.setLocationRelativeTo(gui);
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
