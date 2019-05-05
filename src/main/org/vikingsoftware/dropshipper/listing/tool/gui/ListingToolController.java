package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.workers.FulfillmentListingParserWorker;

public class ListingToolController {

	private static final DecimalFormat decimalFormat = new DecimalFormat("###.##");

	private final ListingToolGUI gui = ListingToolGUI.get();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private DefaultListModel<BufferedImage> imagesModel;
	private double originalListingPrice;

	public void setup() {
		SwingUtilities.invokeLater(() -> {
			addListeners();
			addImageList();
		});
	}

	private void addListeners() {
		gui.listingURLInput.addKeyListener(createFulfillmentUrlKeyAdapter());

        gui.descRawInput.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
        		executor.submit(() -> {
        			try {
						Thread.sleep(200);
					} catch (final InterruptedException e1) {
						e1.printStackTrace();
					}
        			SwingUtilities.invokeLater(() -> gui.descHtmlView.setText(gui.descRawInput.getText()));
        		});
        	}
        });

        gui.skipListingBtn.addActionListener(e -> {
        	ListingQueue.poll();
        	displayNextListing();
        });
        gui.publishListingBtn.addActionListener(e -> publishListing());

        gui.profitMarginInput.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
            	updateListingPriceWithMargin();
        	}
        });
        gui.listingPriceInput.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
            	updateMarginWithPrice();
        	}
        });

        gui.shippingPriceInput.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyTyped(KeyEvent e) {
        		updateMarginWithPrice();
        	}
        });

        gui.fullfillmentsPanelFileBtn.addActionListener(e -> importFile());

	}

	private void addImageList() {
		System.out.println("addImageList from EDT: " + SwingUtilities.isEventDispatchThread());
		imagesModel = new DefaultListModel<>();
		final JList<BufferedImage> imageList = new JList<>(imagesModel);
		imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		imageList.setCellRenderer(new IconCellRenderer());
		gui.imagesPanel.add(imageList);
		gui.imagesPanel.revalidate();
	}

	public void displayNextListing() {
		final Listing listing = ListingQueue.peek();
		imagesModel.removeAllElements();
		if(listing != null) {
			SwingUtilities.invokeLater(() -> {
				gui.listingTitleInput.setText(listing.title);
				gui.descRawInput.setText(listing.description);
				gui.descHtmlView.setText(listing.description);
				gui.listingPriceInput.setText("$" + listing.price);
				if(gui.profitMarginInput.getText().isEmpty()) {
					gui.profitMarginInput.setText("-20%");
				}

				addImages(listing.pictures);
				originalListingPrice = listing.price;
				updateListingPriceWithMargin();
			});
		} else {
			SwingUtilities.invokeLater(() -> {
				gui.listingTitleInput.setText("");
				gui.descRawInput.setText("");
				gui.descHtmlView.setText("");
				gui.listingPriceInput.setText("");
				gui.profitMarginInput.setText("");

				originalListingPrice = 0;
				updateListingPriceWithMargin();
			});
		}
	}

	private void addImages(final Map<String, BufferedImage> images) {
		for(final BufferedImage image : images.values()) {
			final BufferedImage resizedImage = resize(image, 300);
			imagesModel.addElement(resizedImage);
		}
	}

	private void updateMarginWithPrice() {
		try {
			final String listingPriceTxt = gui.listingPriceInput.getText().replace("$", "");
			final String shippingPriceTxt = gui.shippingPriceInput.getText().replace("$", "");
			final double currentPrice = Double.parseDouble(listingPriceTxt) + Double.parseDouble(shippingPriceTxt);
			final String margin = decimalFormat.format(((currentPrice - (originalListingPrice * 1.20)) / currentPrice) * 100);
			gui.profitMarginInput.setText(margin + "%");
		} catch(final Exception e) {
			//swallow exception
		}
	}

    private void updateListingPriceWithMargin() {
    	try {
    		final String marginText = gui.profitMarginInput.getText().replace("%", "");
	    	final String shippingPriceTxt = gui.shippingPriceInput.getText().replace("$", "");
	    	final double price = originalListingPrice * (1.20 + (Double.parseDouble(marginText) / 100))
	    			- (!shippingPriceTxt.isEmpty() ? Double.parseDouble(shippingPriceTxt) : 0);
	    	final String priceWithMargin = decimalFormat.format(price);
	    	gui.listingPriceInput.setText("$" + priceWithMargin);
    	} catch(final Exception e) {
    		//swallow exception
    	}
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
					System.out.println("Attempting to add fulfillment URL to queue: " + gui.listingURLInput.getText());
					FulfillmentListingParserWorker.instance().addUrlToQueue(gui.listingURLInput.getText());
					SwingUtilities.invokeLater(() -> gui.listingURLInput.setText(""));
				}
			}
		};
	}

	private void importFile() {
		final JFileChooser fileChooser = new JFileChooser();
        final int returnVal = fileChooser.showOpenDialog(gui);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            handleImportFile(file);
        }
	}

	private void handleImportFile(final File file) {
		try(final FileReader fR = new FileReader(file);
			final BufferedReader bR = new BufferedReader(fR)) {
			String line;
			while((line = bR.readLine()) != null) {
				try {
					new URL(line);
					FulfillmentListingParserWorker.instance().addUrlToQueue(line);
				} catch(final MalformedURLException e) {
					System.out.println("Encountered malformed url: " + line);
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Takes a BufferedImage and resizes it according to the provided targetSize
	 *
	 * @param src the source BufferedImage
	 * @param targetSize maximum height (if portrait) or width (if landscape)
	 * @return a resized version of the provided BufferedImage
	 */
	private BufferedImage resize(BufferedImage src, int targetSize) {
	    if (targetSize <= 0) {
	        return src; //this can't be resized
	    }
	    int targetWidth = targetSize;
	    int targetHeight = targetSize;
	    final float ratio = ((float) src.getHeight() / (float) src.getWidth());
	    if (ratio <= 1) { //square or landscape-oriented image
	        targetHeight = (int) Math.ceil(targetWidth * ratio);
	    } else { //portrait image
	        targetWidth = Math.round(targetHeight / ratio);
	    }
	    final BufferedImage bi = new BufferedImage(targetWidth, targetHeight, src.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
	    final Graphics2D g2d = bi.createGraphics();
	    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); //produces a balanced resizing (fast and decent quality)
	    g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
	    g2d.dispose();
	    return bi;
	}

}
