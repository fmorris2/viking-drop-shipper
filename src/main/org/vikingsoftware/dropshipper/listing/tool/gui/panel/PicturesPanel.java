package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PicturesPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final JScrollPane scrollPane = new JScrollPane();
	private final JPanel panel = new JPanel();
	
	public PicturesPanel() {
		panel.setLayout(new GridLayout(0, 2));
		scrollPane.add(panel);
		super.add(scrollPane);
	}
	
	
}
