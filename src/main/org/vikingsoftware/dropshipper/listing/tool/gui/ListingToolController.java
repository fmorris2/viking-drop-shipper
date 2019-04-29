package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.workers.FulfillmentListingParserWorker;

public class ListingToolController {

	private final ListingToolGUI gui = ListingToolGUI.get();

	public void addListeners() {
		gui.fulfillmentUrlInput.addKeyListener(createFulfillmentUrlKeyAdapter());
	}

	private KeyAdapter createFulfillmentUrlKeyAdapter() {
		return new KeyAdapter() {

			@Override
			public void keyReleased(final KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					System.out.println("Attempting to add fulfillment URL to queue: " + gui.fulfillmentUrlInput.getText());
					FulfillmentListingParserWorker.instance().addUrlToQueue(gui.fulfillmentUrlInput.getText());
					SwingUtilities.invokeLater(() -> gui.fulfillmentUrlInput.setText(""));
				}
			}
		};
	}

}
