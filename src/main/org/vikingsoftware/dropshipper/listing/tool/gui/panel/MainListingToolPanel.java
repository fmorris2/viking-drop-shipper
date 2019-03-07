package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import java.awt.Dimension;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class MainListingToolPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_WIDTH = 800;
	private static final int DEFAULT_HEIGHT = 800;
	
	public MainListingToolPanel() {
		super(new MigLayout("debug"));
		this.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
	}

}
