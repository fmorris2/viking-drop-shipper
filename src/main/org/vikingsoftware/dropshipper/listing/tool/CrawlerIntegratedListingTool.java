package main.org.vikingsoftware.dropshipper.listing.tool;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;

public class CrawlerIntegratedListingTool {
	
	public static void main(final String[] args) {
		final ListingToolGUI gui = ListingToolGUI.get();
		gui.setVisible(true);
		gui.startCrawler();
	}
}
