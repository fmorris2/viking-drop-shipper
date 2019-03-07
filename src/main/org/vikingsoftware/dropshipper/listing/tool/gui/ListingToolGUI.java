package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import javax.swing.JFrame;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.panel.MainListingToolPanel;

public class ListingToolGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String TITLE = "Viking DS Listing Tool";
	
	public ListingToolGUI() {
		this.setTitle(TITLE);
		this.add(new MainListingToolPanel());
	}

}
