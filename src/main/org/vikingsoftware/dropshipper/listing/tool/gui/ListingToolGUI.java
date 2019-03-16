package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import javax.swing.JFrame;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.panel.FileMenuBar;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.panel.MainListingToolPanel;
import net.miginfocom.swing.MigLayout;

public class ListingToolGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String TITLE = "Viking DS Listing Tool";
	private static final int DEFAULT_WIDTH = 1000;
	private static final int DEFAULT_HEIGHT = 700;
	
	public ListingToolGUI() {
		super.setLayout(new MigLayout("fill"));
		super.setTitle(TITLE);
		super.setJMenuBar(new FileMenuBar());
		super.add(new MainListingToolPanel(), "grow");
		super.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

}
