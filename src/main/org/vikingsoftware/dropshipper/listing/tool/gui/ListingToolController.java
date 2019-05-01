package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.workers.FulfillmentListingParserWorker;

public class ListingToolController {

	private static final DecimalFormat decimalFormat = new DecimalFormat("###.##");

	private final ListingToolGUI gui = ListingToolGUI.get();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private double originalListingPrice;

	public void addListeners() {
		gui.fulfillmentUrlInput.addKeyListener(createFulfillmentUrlKeyAdapter());

        gui.rawDescInput.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
        		executor.submit(() -> {
        			try {
						Thread.sleep(200);
					} catch (final InterruptedException e1) {
						e1.printStackTrace();
					}
        			SwingUtilities.invokeLater(() -> gui.renderedDescPane.setText(gui.rawDescInput.getText()));
        		});
        	}
        });

        gui.skipListingBtn.addActionListener(e -> {
        	ListingQueue.poll();
        	displayNextListing();
        });
        gui.publishListingBtn.addActionListener(e -> publishListing());

        ((JSpinner.DefaultEditor)gui.targetMarginSpinner.getEditor()).getTextField().addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
            	updateListingPriceWithMargin();
        	}
        });
        ((JSpinner.DefaultEditor)gui.priceSpinner.getEditor()).getTextField().addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
            	updateMarginWithPrice();
        	}
        });

	}

	public void displayNextListing() {
		final Listing listing = ListingQueue.peek();
		if(listing != null) {
			SwingUtilities.invokeLater(() -> {
				gui.listingTitleInput.setText(listing.title);
				gui.rawDescInput.setText(listing.description);
				gui.renderedDescPane.setText(listing.description);

				originalListingPrice = listing.price;
				updateListingPriceWithMargin();
			});
		} else {
			SwingUtilities.invokeLater(() -> {
				gui.listingTitleInput.setText("");
				gui.rawDescInput.setText("");
				gui.renderedDescPane.setText("");

				originalListingPrice = 0;
				updateListingPriceWithMargin();
			});
		}
	}

	private void updateMarginWithPrice() {
		final double currentPrice = (double)gui.priceSpinner.getValue() * .80;
		final String margin = decimalFormat.format(((currentPrice - originalListingPrice) / originalListingPrice) * 100);

		gui.targetMarginSpinner.setValue(Double.parseDouble(margin));
	}

    private void updateListingPriceWithMargin() {
    	final String priceWithMargin = decimalFormat.format(originalListingPrice * (1.13 + ((double)gui.targetMarginSpinner.getValue() / 100)));
    	gui.priceSpinner.setValue(Double.parseDouble(priceWithMargin));
    }

	private void publishListing() {
		final Listing listing = ListingQueue.poll();
		//publish...
		displayNextListing();
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
