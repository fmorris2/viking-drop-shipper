package main.org.vikingsoftware.dropshipper.listing.tool.gui.panel;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import main.org.vikingsoftware.dropshipper.listing.tool.ListingToolData;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParsingManager;

public class FileMenu extends JMenu {
	
	private static final long serialVersionUID = 1L;
	
	private final JMenuItem loadURL = new JMenuItem("Load URL");
	private final JMenuItem loadFile = new JMenuItem("Load File");
	
	public FileMenu() {
		super("File");
		loadURL.addActionListener(this::loadURL);
		loadFile.addActionListener(this::loadFile);
		super.add(loadURL);
		super.add(loadFile);
	}
	
	private void loadURL(final ActionEvent e) {
		final String url = JOptionPane.showInputDialog("Enter a URL to load:");
		System.out.println("URL to load: " + url);
		final SwingWorker<Listing, Void> loadingWorker = generateLoadListingWorker(url);
		loadingWorker.execute();
	}
	
	private void loadFile(final ActionEvent e) {
		
	}
	
	private SwingWorker<Listing, Void> generateLoadListingWorker(final String url) {
		
		return new SwingWorker<Listing, Void>() {

			@Override
			protected Listing doInBackground() throws Exception {
				return FulfillmentParsingManager.parseListing(url);
			}
			
			@Override
			protected void done() {
				try {
					ListingToolData.get().addListingToQueue(get());
					if(ListingToolData.get().getCurrentListing() == null) {
						MainListingToolPanel.setListing(ListingToolData.get().nextListing());
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			
		};
	}

}
