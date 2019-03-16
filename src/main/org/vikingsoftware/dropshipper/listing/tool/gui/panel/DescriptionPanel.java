package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

public class DescriptionPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final JEditorPane viewArea = new JEditorPane();
	private final JScrollPane viewScroll = new JScrollPane(viewArea);
	
	public DescriptionPanel() {
		super.setLayout(new MigLayout("fill"));
		viewArea.setContentType("text/html");
		viewArea.setEditable(false);
		super.add(viewScroll, "grow");
	}
	
	public void setDescription(final String description) {
		viewArea.setText(description);
	}

}
