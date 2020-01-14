package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentManager;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.MarketplaceLoader;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.Marketplaces;
import main.org.vikingsoftware.dropshipper.core.data.marketplace.listing.MarketplaceListing;
import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayCalls;
import main.org.vikingsoftware.dropshipper.core.utils.PriceUtils;
import main.org.vikingsoftware.dropshipper.crawler.FulfillmentListingCrawler;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.EbayCategory;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.workers.FulfillmentListingParserWorker;
import main.org.vikingsoftware.dropshipper.listing.tool.types.DocumentAdapter;

public class ListingToolController {

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");
	private static final String BASE_EBAY_SEARCH_URL = "https://www.m.ebay.com/sch/i.html?_nkw=";
	private static final String BASE_EBAY_SEARCH_URL_SOLD_ITEMS = "https://www.m.ebay.com/sch/i.html?LH_Sold=1&LH_Complete=1&_nkw=";
	private static final double DEFAULT_AUTOMATED_MARGIN = 15.0;
	private static final Set<String> BLACKLISTED_AUTOMATED_CATEGORIES = new HashSet<>(Arrays.asList(
	   "video game",
	   "cell phone",
	   "gift card",
	   "clothing",
	   "gaming",
	   "prepaid"
	));
	
	private final ListingToolGUI gui = ListingToolGUI.get();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final FulfillmentListingCrawler crawler = new FulfillmentListingCrawler();
	private final Set<String> publishedItemIds = new HashSet<>();

	private JList<BufferedImage> imageList;
	private DefaultListModel<BufferedImage> imagesModel;
	private DefaultComboBoxModel<EbayCategory> categoryModel;
	private RecentSalesRenderer recentSalesRenderer;
	private Listing currentListingClone;
	private double originalListingPrice;
	private boolean isPublishing;
	private boolean isAutomated;
	
	public void setup() {
		SwingUtilities.invokeLater(() -> {
			addListeners();
			addImageList();
			addRecentSalesRenderer();
			addCategoryModel();
		});
	}
	
	public void shuffle() {
		FulfillmentListingParserWorker.instance().shuffle();
		ListingQueue.shuffle();
		displayNextListing();
	}
	
	public void setAutomated(final boolean automated) {
		this.gui.setVisible(false);
		this.isAutomated = automated;
	}
	
	public void startCrawler() {
		System.out.println("Starting Crawler");
		crawler.start(this::addFulfillmentURLToQueue);
	}

	private void addListeners() {
		gui.fulfillmentsPanelInput.addKeyListener(createFulfillmentUrlKeyAdapter());

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
        
        gui.listingTitleInput.getDocument().addDocumentListener(new DocumentAdapter() {
        	@Override
        	public void insertUpdate(DocumentEvent e) {
        		checkListingTitleLength();
        	}
        	
        	public void removeUpdate(DocumentEvent e) {
        		checkListingTitleLength();
        	};
        });

        gui.skipListingBtn.addActionListener(e -> {
        	ListingQueue.poll();
        	displayNextListing();
        });
        gui.publishListingBtn.addActionListener(e -> publishListing());

        gui.profitMarginInput.getDocument().addDocumentListener(new DocumentAdapter() {
        	@Override
        	public void insertUpdate(DocumentEvent evt) {
        		updateListingPriceWithMargin();
        	}
        	@Override
        	public void removeUpdate(DocumentEvent evt) {
        		updateListingPriceWithMargin();
    		}
        });

        gui.listingPriceInput.getDocument().addDocumentListener(new DocumentAdapter() {
        	@Override
        	public void insertUpdate(DocumentEvent evt) {
        		updateMarginWithPrice();
        	}
        	@Override
			public void removeUpdate(DocumentEvent evt) {
        		updateMarginWithPrice();
        	}
        });

        gui.shippingPriceInput.getDocument().addDocumentListener(new DocumentAdapter() {
        	@Override
        	public void insertUpdate(DocumentEvent evt) {
        		updateMarginWithPrice();
        	}
        	@Override
			public void removeUpdate(DocumentEvent evt) {
        		updateMarginWithPrice();
        	}
        });

        gui.fullfillmentsPanelFileBtn.addActionListener(e -> importFile());
        gui.soldItemsCheckbox.addActionListener(e -> updateRecentSoldItemsBrowser());
        gui.resetListingInformation.addActionListener(e -> {
        	displayListing(currentListingClone);
        	ListingQueue.replaceFirst(currentListingClone);
        });

	}
	
	private void checkListingTitleLength() {
		if(gui.listingTitleInput.getText().length() > 80) {
			gui.listingTitleInput.setForeground(Color.RED);
		} else {
			gui.listingTitleInput.setForeground(Color.BLACK);
		}
	}

	private void addImageList() {
		imagesModel = new DefaultListModel<>();
		imageList = new JList<>(imagesModel);
		imageList.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e)) {
		            final int row = imageList.locationToIndex(e.getPoint());
		            imageList.setSelectedIndex(row);
					handleImageListRightClick(e);
				}
			}
		});
		imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		imageList.setCellRenderer(new IconCellRenderer());
		gui.imagesPanel.add(imageList);
		gui.imagesPanel.revalidate();
	}

	private void handleImageListRightClick(final MouseEvent evt) {
		final int selected = imageList.getSelectedIndex();
		final JPopupMenu menu = new JPopupMenu();

		if(selected > 0) {
			final JMenuItem moveUp = new JMenuItem("Move up");
			moveUp.addActionListener(e -> moveSelectedImageUp());
			menu.add(moveUp);
		}

		if(selected < imagesModel.getSize() - 1) {
			final JMenuItem moveDown = new JMenuItem("Move down");
			moveDown.addActionListener(e -> moveSelectedImageDown());
			menu.add(moveDown);
		}

		final JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e -> deleteSelectedImage());

		menu.add(delete);
		menu.show(gui.imagesPanel, evt.getPoint().x, evt.getPoint().y);
	}

	private void moveSelectedImage(final int direction) {
		final int selected = imageList.getSelectedIndex();
		final Listing listing = ListingQueue.peek();
		final ListingImage toMove = listing.pictures.remove(selected);
		listing.pictures.add(selected + direction, toMove);
		imagesModel.removeAllElements();
		addImages(listing.pictures);
	}

	private void moveSelectedImageUp() {
		moveSelectedImage(-1);
	}

	private void moveSelectedImageDown() {
		moveSelectedImage(1);
	}

	private void deleteSelectedImage() {
		ListingQueue.peek().pictures.remove(imageList.getSelectedIndex());
		imagesModel.remove(imageList.getSelectedIndex());
	}

	private void addRecentSalesRenderer() {
//		recentSalesRenderer = new RecentSalesRenderer();
//		recentSalesRenderer.get("http://www.google.com");
	}

	@SuppressWarnings("unchecked")
	private void addCategoryModel() {
		categoryModel = new DefaultComboBoxModel<>();
		gui.categoryDropdown.setModel(categoryModel);
	}

	public void displayNextListing() {
		if(ListingQueue.peek() != null && publishedItemIds.contains(ListingQueue.peek().itemId)) {
			System.out.println("Skipping already published item id: " + ListingQueue.peek().itemId);
			ListingQueue.poll();
			displayNextListing();
			return;
		}
		displayListing(ListingQueue.peek());
	}

	public void displayListing(final Listing listing) {
		System.out.println("displayListing: " + listing);
		if(this.isAutomated && listing != null) {
			setListingModel(listing);
			publishListing();
		} else {
			imagesModel.removeAllElements();
			if(listing != null) {
				setGUI(listing);
			} else {
				clearGUI();
			}
			gui.listingPriceInput.requestFocus();
		}
	}

	private void setGUI(final Listing listing) {
		SwingUtilities.invokeLater(() -> {
			setListingModel(listing);
			updateRecentSoldItemsBrowser();
			gui.listingTitleInput.setText(listing.title);
			gui.descRawInput.setText(listing.description);
			gui.descHtmlView.setText(listing.description);
			gui.brandInput.setText(listing.brand);
			updateListingPriceWithMargin();
			addImages(listing.pictures);
		});
	}
	
	private void setListingModel(final Listing listing) {
		originalListingPrice = listing.price;
		currentListingClone = listing.clone();
		updateCategoryModel(listing);
	}

	private void clearGUI() {
		SwingUtilities.invokeLater(() -> {
			originalListingPrice = 0;
			gui.listingTitleInput.setText("");
			gui.descRawInput.setText("");
			gui.descHtmlView.setText("");
			gui.listingPriceInput.setText("");
			gui.profitMarginInput.setText("");
			gui.brandInput.setText("");
//			recentSalesRenderer.get("http://www.google.com");
			categoryModel.removeAllElements();
			updateListingPriceWithMargin();
		});
	}

	private void updateCategoryModel(final Listing listing) {
		final EbayCategory[] suggestedCategories = EbayCalls.getSuggestedCategories(listing.title);
		categoryModel.removeAllElements();
		for(final EbayCategory category : suggestedCategories) {
			categoryModel.addElement(category);
		}
	}

	private void updateRecentSoldItemsBrowser() {
		try {
			if(ListingQueue.peek() != null) {
				final String baseUrl = gui.soldItemsCheckbox.isSelected() ? BASE_EBAY_SEARCH_URL_SOLD_ITEMS : BASE_EBAY_SEARCH_URL;
				final String url = baseUrl + URLEncoder.encode(ListingQueue.peek().title, "UTF-8");
				System.out.println("Setting recent sold items browser to url: " + url);
//				recentSalesRenderer.get(url);
			}
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void addImages(final List<ListingImage> images) {
		for(final ListingImage image : images) {
			final BufferedImage resizedImage = resize(image.getImage(), 300);
			imagesModel.addElement(resizedImage);
		}
	}

	private void updateMarginWithPrice() {
		try {
			final String listingPriceTxt = gui.listingPriceInput.getText().isEmpty() ? "0.00" : gui.listingPriceInput.getText().replace("$", "");
			final String shippingPriceTxt = gui.shippingPriceInput.getText().isEmpty() ? "0.00" : gui.shippingPriceInput.getText().replace("$", "");
			final double sellPrice = Double.parseDouble(listingPriceTxt) + Double.parseDouble(shippingPriceTxt);
			final double marginPercentage = PriceUtils.getMarginPercentage(originalListingPrice, sellPrice);
			
			final String marginString = DECIMAL_FORMAT.format(marginPercentage);
			gui.profitMarginInput.setText(marginString + "%");
		} catch(final Exception e) {
			//swallow exception
		}
	}

    private void updateListingPriceWithMargin() {
    	try {
    		final String marginText = gui.profitMarginInput.getText().isEmpty() ? "0.00" : gui.profitMarginInput.getText().replace("%", "");
	    	final String shippingPriceTxt = gui.shippingPriceInput.getText().isEmpty() ? "0.00" : gui.shippingPriceInput.getText().replace("$", "");
	    	final double shippingPrice = Double.parseDouble(shippingPriceTxt);
	    	final double margin = Double.parseDouble(marginText);
	    	final double priceFromMargin = PriceUtils.getPriceFromMargin(originalListingPrice, shippingPrice, margin);
	    	final String priceWithMargin = DECIMAL_FORMAT.format(priceFromMargin);
	    	gui.listingPriceInput.setText("$" + priceWithMargin);
    	} catch(final Exception e) {
    		//swallow exception
    	}
    }

	private void publishListing() {
		if(!isPublishing) {
			isPublishing = true;
			try {
				if(isAutomated) {
					publishListingImpl();
				} else {
					executor.execute(() -> {
						publishListingImpl();
					});
				}
			} finally {
				isPublishing = false;
			}
		}
	}
	
	private void publishListingImpl() {
		//publish...
		final Listing toPublish = createListingToPublish();
		final boolean isAlreadyListed = FulfillmentManager.get().getListingForItemId(toPublish.fulfillmentPlatformId, toPublish.itemId).isPresent();
		if(toPublish != null && verifyRequiredItemSpecifics(toPublish) && !isAlreadyListed
				&& toPublish.title.length() <= 80) {
			final Optional<String> publishedEbayListingItemId = EbayCalls.createListing(toPublish);
			if(publishedEbayListingItemId.isPresent()) {
				System.out.println("Successfully published eBay listing: " + toPublish.title);
				connectListingInDB(toPublish, publishedEbayListingItemId.get());
				publishedItemIds.add(toPublish.itemId);
			} else {
				System.out.println("Failed to publish eBay listing for listing: " + toPublish.title);
			}
		}
		ListingQueue.poll();
		displayNextListing();
	}
	
	private boolean verifyRequiredItemSpecifics(final Listing listing) {
		if(listing.requiredItemSpecifics.isEmpty()) {
			System.out.println("Listing has no required eBay item specifics - Successfully passed verification");
			return true;
		}
		
		final Map<String, String> results = ItemSpecificsPanelManager.get().getRequiredItemSpecifics(gui, listing);
		
		if(results.isEmpty() || results.size() != listing.requiredItemSpecifics.size()) {
			System.out.println("Listing has " + listing.requiredItemSpecifics.size() + " and we've provided " + results.size() + ". Failing verification.");
			return false;
		}
		
		System.out.println("We've provided the required " + listing.requiredItemSpecifics.size() + " required item specifics. Passing verification.");
		listing.itemSpecifics = results;
		return true;
	}

	private void connectListingInDB(final Listing listing, final String listingId) {
		try {
			if(insertMarketplaceListing(listing, listingId) && insertFulfillmentListing(listing)) {
				final MarketplaceListing marketplaceListing = MarketplaceLoader.loadMarketplaceListingByListingId(listingId);
				final FulfillmentListing fulfillmentListing = FulfillmentManager.get().getListingForItemId(listing.fulfillmentPlatformId, listing.itemId).get();
				if(insertFulfillmentMapping(marketplaceListing, fulfillmentListing)) {
					System.out.println("Successfully connected listing in DB: " + listing.title);
				} else {
					System.out.println("FAILED TO CONNECT LISTING IN DB: " + listing.title);
					System.exit(0);
				}
			} else {
				System.out.println("FAILED TO CONNECT LISTING IN DB: " + listing.title);
				System.exit(0);
			}
		} catch(final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private boolean insertMarketplaceListing(final Listing listing, final String listingId) {
		try {
			final String query = "INSERT INTO marketplace_listing(marketplace_id, listing_id, listing_title,"
					+ " fulfillment_quantity_multiplier, active, target_margin, current_shipping_cost,"
					+ " current_price, target_handling_time, last_margin_update, current_handling_time) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
			final PreparedStatement statement = VSDSDBManager.get().createPreparedStatement(query);
			statement.setInt(1, Marketplaces.EBAY.getMarketplaceId());
			statement.setString(2, listingId);
			statement.setString(3, listing.title);
			statement.setInt(4, 1);
			statement.setInt(5, 1);
			statement.setDouble(6, listing.targetProfitMargin);
			statement.setDouble(7, listing.shipping);
			statement.setDouble(8, listing.price);
			statement.setInt(9, listing.handlingTime);
			statement.setLong(10, System.currentTimeMillis());
			statement.setInt(11, listing.handlingTime);
			statement.execute();
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private boolean insertFulfillmentListing(final Listing listing) {
		try {
			final String query = "INSERT INTO fulfillment_listing(fulfillment_platform_id, item_id, product_id, listing_title, listing_url,"
					+ "upc,ean)"
					+ " VALUES(?,?,?,?,?,?,?)";
			final PreparedStatement statement = VSDSDBManager.get().createPreparedStatement(query);
			statement.setInt(1, listing.fulfillmentPlatformId);
			statement.setString(2, listing.itemId);
			statement.setString(3, listing.productId);
			statement.setString(4, listing.title);
			statement.setString(5, listing.url);
			statement.setString(6, listing.upc);
			statement.setString(7, listing.ean);
			statement.execute();
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean insertFulfillmentMapping(final MarketplaceListing marketListing, final FulfillmentListing fulfillmentListing) {
		try {
			final String query = "INSERT INTO fulfillment_mapping(marketplace_listing_id, fulfillment_listing_id) VALUES(?,?)";
			final PreparedStatement statement = VSDSDBManager.get().createPreparedStatement(query);
			statement.setInt(1, marketListing.id);
			statement.setInt(2, fulfillmentListing.id);
			statement.execute();
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private Listing createListingToPublish() {
		if(!ListingQueue.isEmpty()) {
			final Listing toPublish = ListingQueue.peek().clone();
			if(isAutomated) {
				System.out.println("Getting suggested categories for title " + toPublish.title);
				final EbayCategory[] suggestedCategories = EbayCalls.getSuggestedCategories(toPublish.title);
				System.out.println("Received " + suggestedCategories.length + " suggested categories");
				if(suggestedCategories == null || suggestedCategories.length == 0) {
					return null;
				}
				
				for(final String blacklistedCategory : BLACKLISTED_AUTOMATED_CATEGORIES) {
					if(suggestedCategories[0].name.toLowerCase().contains(blacklistedCategory)) {
						return null;
					}
				}
				toPublish.category = suggestedCategories[0];
				toPublish.price = PriceUtils.getPriceFromMargin(originalListingPrice, 0, DEFAULT_AUTOMATED_MARGIN);
				toPublish.shipping = 0D;
				toPublish.targetProfitMargin = DEFAULT_AUTOMATED_MARGIN;
			} else {
				toPublish.title = gui.listingTitleInput.getText().trim();
				toPublish.price = Double.parseDouble(gui.listingPriceInput.getText().replace("$", "").trim());
				if(!gui.shippingPriceInput.getText().isEmpty()) {
					toPublish.shipping = Double.parseDouble(gui.shippingPriceInput.getText().replace("$", "").trim());
				}
				toPublish.targetProfitMargin = Double.parseDouble(gui.profitMarginInput.getText().replace("%", "").trim());
				toPublish.description = gui.descRawInput.getText();
				toPublish.category = (EbayCategory)gui.categoryDropdown.getSelectedItem();
				toPublish.brand = gui.brandInput.getText();
			}
			
			toPublish.requiredItemSpecifics = EbayCalls.getRequiredItemSpecificFields(toPublish.category.id);
			return toPublish;
		}
		
		return null;
	}

	private KeyAdapter createFulfillmentUrlKeyAdapter() {
		return new KeyAdapter() {

			@Override
			public void keyReleased(final KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					addFulfillmentURLToQueue(gui.fulfillmentsPanelInput.getText());
				}
			}
		};
	}
	
	private void addFulfillmentURLToQueue(final String url) {
		if(url != null && !url.isEmpty()) {
			System.out.println("Attempting to add fulfillment URL to queue: " + url);
			FulfillmentListingParserWorker.instance().addUrlToQueue(url);
			SwingUtilities.invokeLater(() -> gui.fulfillmentsPanelInput.setText(""));
		}
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
