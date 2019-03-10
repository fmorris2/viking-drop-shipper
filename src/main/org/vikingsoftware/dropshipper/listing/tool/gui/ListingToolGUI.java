package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import javax.swing.JFrame;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.panel.FileMenuBar;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.panel.MainListingToolPanel;

public class ListingToolGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_WIDTH = 800;
	private static final int DEFAULT_HEIGHT = 800;
	private static final String TITLE = "Viking DS Listing Tool";
	
	public ListingToolGUI() {
		super.setTitle(TITLE);
		//super.setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)); //make at least default size
		super.setJMenuBar(new FileMenuBar());
		super.add(new MainListingToolPanel());
		super.pack();
		super.setVisible(true);
	}

}
