package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import java.awt.TextField;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import net.miginfocom.swing.MigLayout;

public class MainListingToolPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private static final PicturesPanel picturesPanel = new PicturesPanel();
	private static final JButton listItButton = new JButton("List it!");
	private static final DescriptionPanel descriptionPanel = new DescriptionPanel();
	private static final TextField titleField = new TextField();
	private static final VariationsPanel variationsPanel = new VariationsPanel();
	private static final JComboBox<String> categoryBox = new JComboBox<>();
	private static final JButton skipItButton = new JButton("Skip it!");
	
	public MainListingToolPanel() {
		super(buildLayout());
		//cell column row width height
		super.add(picturesPanel, "cell 0 0 1 2, grow");
		super.add(titleField, "cell 1 0 1 1, grow");
		super.add(listItButton, "cell 2 0 1 2");
		super.add(descriptionPanel, "cell 1 1 1 1, grow");
		super.add(variationsPanel, "cell 0 2 1 2, grow");
		super.add(categoryBox, "cell 1 2 1 1, grow");
		super.add(skipItButton, "cell 2 2 1 2");	
	}
	
	public static void setListing(final Listing listing) {
		titleField.setText(listing.title);
		descriptionPanel.setDescription(listing.description);
	}
	
	private static MigLayout buildLayout() {
		final MigLayout layout = new MigLayout("debug, fill");
		layout.setColumnConstraints("[][][10:20:70]");
		layout.setRowConstraints("[][][][]");
		return layout;
	}

}
