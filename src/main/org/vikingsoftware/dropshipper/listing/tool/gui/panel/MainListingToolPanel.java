package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import java.awt.TextArea;
import java.awt.TextField;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

public class MainListingToolPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private final JLabel statusLabel = new JLabel("Status");
	private final PicturesPanel picturesPanel = new PicturesPanel();
	private final JButton listItButton = new JButton("List it!");
	private final TextField titleField = new TextField();
	private final TextArea descriptionArea = new TextArea();
	private final VariationsPanel variationsPanel = new VariationsPanel();
	private final JComboBox<String> categoryBox = new JComboBox<>();
	private final JButton skipItButton = new JButton("Skip it!");
	
	public MainListingToolPanel() {
		super(buildLayout());
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		//cell column row width height
		super.add(statusLabel, "cell 0 0 3 1, grow");
		super.add(picturesPanel, "cell 0 1 1 2, grow");
		super.add(titleField, "cell 1 1 1 1, grow");
		super.add(listItButton, "cell 2 1 1 2, grow");
		super.add(descriptionArea, "cell 1 2 1 1, grow");
		super.add(variationsPanel, "cell 0 3 1 2, grow");
		super.add(categoryBox, "cell 1 3 1 1, grow");
		super.add(skipItButton, "cell 2 3 1 2, grow");	
	}
	
	private static MigLayout buildLayout() {
		final MigLayout layout = new MigLayout("debug, fill");
		layout.setColumnConstraints("[][][]");
		layout.setRowConstraints("[][][][][]");
		return layout;
	}

}
