package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class FileMenu extends JMenu {
	
	private static final long serialVersionUID = 1L;
	
	public FileMenu() {
		super("File");
		super.add(new JMenuItem("Load URL"));
		super.add(new JMenuItem("Load File"));
	}

}
