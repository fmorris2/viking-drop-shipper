package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class VariationsPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private final JScrollPane scrollPane = new JScrollPane();
	private final JTable table = new JTable();
	
	public VariationsPanel() {
		scrollPane.add(table);
		super.add(scrollPane);
	}

}
