/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.Dimension;

import javax.swing.JButton;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingQueue;

/**
 *
 * @author Bren
 */
public class ListingToolGUI extends javax.swing.JFrame {

	private static final long serialVersionUID = 7954579463528571026L;
	private static final String TITLE = "Viking Software Drop Shipper - Semi-Auto Zombie Lister";

	private static ListingToolGUI instance;
	private static ListingToolController controller;

	/**
     * Creates new form GUI
     */
    private ListingToolGUI() {
        initComponents();
        descRawScrollPane.setMinimumSize(new Dimension(667, 252));
        descHtmlScrollPane.setMinimumSize(new Dimension(667, 400));
//        recentSalesScrollPane.setMinimumSize(new Dimension(335, recentSalesPanel.getHeight()));
//        recentSalesScrollPane.setMaximumSize(new Dimension(335, 800));
//        recentSalesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//        recentSalesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        platformDropdown.setEnabled(false);
        setTitle(TITLE);
    }

    public static synchronized ListingToolGUI get() {
    	if(instance == null) {
    		instance = new ListingToolGUI();
    		controller = new ListingToolController();
    		controller.setup();
    	}

    	return instance;
    }

    public static ListingToolController getController() {
    	return controller;
    }
    
    public void startCrawler() {
    	controller.startCrawler();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fulfillmentsPanel = new javax.swing.JPanel();
        fulfillmentsPanelHeaderText = new javax.swing.JLabel();
        fulfillmentsPanelAddLabelText = new javax.swing.JLabel();
        fulfillmentsPanelInput = new javax.swing.JTextField();
        fullfillmentsPanelFileBtn = new javax.swing.JButton();
        queuePanelContainer = new javax.swing.JPanel();
        urlsToParseLabel = new javax.swing.JLabel();
        urlsToParseValue = new javax.swing.JLabel();
        parsedQueueLabel = new javax.swing.JLabel();
        parsedQueueValue = new javax.swing.JLabel();
        generalInfoPanel = new javax.swing.JPanel();
        generalInfoPanelHeaderText = new javax.swing.JLabel();
        listingTitleLabel = new javax.swing.JLabel();
        listingTitleInput = new javax.swing.JTextField();
        listingPriceInput = new javax.swing.JTextField();
        shippingPriceInput = new javax.swing.JTextField();
        profitMarginInput = new javax.swing.JTextField();
        listingPriceLabel = new javax.swing.JLabel();
        shippingPriceLabel = new javax.swing.JLabel();
        profitMarginLabel = new javax.swing.JLabel();
        categoryLabel = new javax.swing.JLabel();
        categoryDropdown = new javax.swing.JComboBox();
        descriptionLabel = new javax.swing.JLabel();
        descRawScrollPane = new javax.swing.JScrollPane();
        descRawInput = new javax.swing.JEditorPane();
        descHtmlScrollPane = new javax.swing.JScrollPane();
        descHtmlView = new javax.swing.JEditorPane();
        resetListingInformation = new javax.swing.JButton();
        brandLabel = new javax.swing.JLabel();
        brandInput = new javax.swing.JTextField();
        statusAreaPanel = new javax.swing.JPanel();
        statusTextValue = new javax.swing.JLabel();
        skipListingBtn = new javax.swing.JButton();
        publishListingBtn = new javax.swing.JButton();
        platformDropdown = new javax.swing.JComboBox();
        platformLabel = new javax.swing.JLabel();
        recentSalesPanelContainer = new javax.swing.JPanel();
        recentSalesHeaderText = new javax.swing.JLabel();
//        recentSalesScrollPane = new javax.swing.JScrollPane();
//        recentSalesPanel = new javax.swing.JPanel();
        soldItemsCheckbox = new javax.swing.JCheckBox();
        shuffleUrlQueue = new JButton("Shuffle URLs");
        shuffleUrlQueue.addActionListener(e -> controller.shuffle());
        imagesPanelContainer = new javax.swing.JPanel();
        imagesPanelHeaderText = new javax.swing.JLabel();
        imagesPanelScrollPane = new javax.swing.JScrollPane();
        imagesPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        fulfillmentsPanelHeaderText.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        fulfillmentsPanelHeaderText.setText("Fulfillments");

        fulfillmentsPanelAddLabelText.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        fulfillmentsPanelAddLabelText.setText("ADD FULFILLMENT URL TO QUEUE");

        fulfillmentsPanelInput.setBackground(new java.awt.Color(252, 247, 233));

        fullfillmentsPanelFileBtn.setBackground(new java.awt.Color(236, 206, 138));
        fullfillmentsPanelFileBtn.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        fullfillmentsPanelFileBtn.setForeground(new java.awt.Color(255, 255, 255));
        fullfillmentsPanelFileBtn.setText("FILE");
        fullfillmentsPanelFileBtn.setFocusable(false);

        urlsToParseLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        urlsToParseLabel.setText("URLs to parse:");

        urlsToParseValue.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        urlsToParseValue.setForeground(new java.awt.Color(0, 0, 255));
        urlsToParseValue.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        urlsToParseValue.setText("0");

        parsedQueueLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        parsedQueueLabel.setText("Parsed queue:");

        parsedQueueValue.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        parsedQueueValue.setForeground(new java.awt.Color(0, 0, 255));
        parsedQueueValue.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        parsedQueueValue.setText("0");

        final javax.swing.GroupLayout queuePanelContainerLayout = new javax.swing.GroupLayout(queuePanelContainer);
        queuePanelContainer.setLayout(queuePanelContainerLayout);
        queuePanelContainerLayout.setHorizontalGroup(
            queuePanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(queuePanelContainerLayout.createSequentialGroup()
                .addComponent(urlsToParseLabel)
                .addGap(0, 0, 0)
                .addComponent(urlsToParseValue, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(queuePanelContainerLayout.createSequentialGroup()
                .addComponent(parsedQueueLabel)
                .addGap(0, 0, 0)
                .addComponent(parsedQueueValue, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        queuePanelContainerLayout.setVerticalGroup(
            queuePanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(queuePanelContainerLayout.createSequentialGroup()
                .addGroup(queuePanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(urlsToParseLabel)
                    .addComponent(urlsToParseValue))
                .addGap(0, 0, 0)
                .addGroup(queuePanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(parsedQueueLabel)
                    .addComponent(parsedQueueValue))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        final javax.swing.GroupLayout fulfillmentsPanelLayout = new javax.swing.GroupLayout(fulfillmentsPanel);
        fulfillmentsPanel.setLayout(fulfillmentsPanelLayout);
        fulfillmentsPanelLayout.setHorizontalGroup(
            fulfillmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fulfillmentsPanelLayout.createSequentialGroup()
                .addGroup(fulfillmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(fulfillmentsPanelLayout.createSequentialGroup()
                        .addGroup(fulfillmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fulfillmentsPanelHeaderText)
                            .addComponent(fulfillmentsPanelAddLabelText))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(queuePanelContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fulfillmentsPanelLayout.createSequentialGroup()
                        .addComponent(fulfillmentsPanelInput)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fullfillmentsPanelFileBtn)))
                .addGap(0, 0, 0))
        );
        fulfillmentsPanelLayout.setVerticalGroup(
            fulfillmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fulfillmentsPanelLayout.createSequentialGroup()
                .addGroup(fulfillmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fulfillmentsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(queuePanelContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(fulfillmentsPanelLayout.createSequentialGroup()
                        .addComponent(fulfillmentsPanelHeaderText)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fulfillmentsPanelAddLabelText)))
                .addGap(7, 7, 7)
                .addGroup(fulfillmentsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(fullfillmentsPanelFileBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                    .addComponent(fulfillmentsPanelInput))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        generalInfoPanelHeaderText.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        generalInfoPanelHeaderText.setText("General Information");

        listingTitleLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        listingTitleLabel.setText("TITLE");

        listingTitleInput.setBackground(new java.awt.Color(252, 247, 233));
        listingTitleInput.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N

        listingPriceInput.setBackground(new java.awt.Color(252, 247, 233));
        listingPriceInput.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        listingPriceInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        shippingPriceInput.setBackground(new java.awt.Color(252, 247, 233));
        shippingPriceInput.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        shippingPriceInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        profitMarginInput.setBackground(new java.awt.Color(252, 247, 233));
        profitMarginInput.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        profitMarginInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        listingPriceLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        listingPriceLabel.setText("PRICE");

        shippingPriceLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        shippingPriceLabel.setText("SHIPPING");

        profitMarginLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        profitMarginLabel.setText("MARGIN");

        categoryLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        categoryLabel.setText("CATEGORY");

        categoryDropdown.setBackground(new java.awt.Color(214, 223, 233));
        categoryDropdown.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        categoryDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "" }));
        categoryDropdown.setFocusable(false);

        descriptionLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        descriptionLabel.setText("DESCRIPTION");

        descRawInput.setFont(new java.awt.Font("Consolas", 0, 11)); // NOI18N
        descRawScrollPane.setViewportView(descRawInput);

        descHtmlView.setEditable(false);
        descHtmlView.setContentType("text/html"); // NOI18N
        descHtmlView.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        descHtmlScrollPane.setViewportView(descHtmlView);

        resetListingInformation.setBackground(new java.awt.Color(51, 153, 255));
        resetListingInformation.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        resetListingInformation.setForeground(new java.awt.Color(255, 255, 255));
        resetListingInformation.setText("Reset to default parsed information");
        resetListingInformation.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        resetListingInformation.setFocusPainted(false);
        resetListingInformation.setFocusable(false);

        brandLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        brandLabel.setText("BRAND");

        brandInput.setBackground(new java.awt.Color(252, 247, 233));
        brandInput.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N

        final javax.swing.GroupLayout generalInfoPanelLayout = new javax.swing.GroupLayout(generalInfoPanel);
        generalInfoPanel.setLayout(generalInfoPanelLayout);
        generalInfoPanelLayout.setHorizontalGroup(
            generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(descRawScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 835, Short.MAX_VALUE)
            .addComponent(descHtmlScrollPane)
            .addGroup(generalInfoPanelLayout.createSequentialGroup()
                .addComponent(descriptionLabel)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(generalInfoPanelLayout.createSequentialGroup()
                .addComponent(generalInfoPanelHeaderText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(soldItemsCheckbox)
                .addComponent(shuffleUrlQueue)
                .addComponent(resetListingInformation))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, generalInfoPanelLayout.createSequentialGroup()
                .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(listingTitleInput)
                    .addComponent(categoryDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(generalInfoPanelLayout.createSequentialGroup()
                        .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(categoryLabel)
                            .addComponent(listingTitleLabel))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(brandLabel)
                    .addGroup(generalInfoPanelLayout.createSequentialGroup()
                        .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(listingPriceLabel)
                            .addComponent(listingPriceInput, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(shippingPriceInput, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(shippingPriceLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(profitMarginLabel)
                            .addComponent(profitMarginInput, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(brandInput)))
        );
        generalInfoPanelLayout.setVerticalGroup(
            generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generalInfoPanelLayout.createSequentialGroup()
                .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(generalInfoPanelHeaderText)
                    .addComponent(soldItemsCheckbox)
                    .addComponent(shuffleUrlQueue)
                    .addComponent(resetListingInformation))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(listingTitleLabel)
                    .addComponent(brandLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(listingTitleInput, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(brandInput, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generalInfoPanelLayout.createSequentialGroup()
                        .addComponent(categoryLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(categoryDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(generalInfoPanelLayout.createSequentialGroup()
                        .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(shippingPriceLabel)
                            .addComponent(profitMarginLabel)
                            .addComponent(listingPriceLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generalInfoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(shippingPriceInput, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                            .addComponent(profitMarginInput)
                            .addComponent(listingPriceInput))))
                .addGap(13, 13, 13)
                .addComponent(descriptionLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(descRawScrollPane)
                .addGap(0, 0, 0)
                .addComponent(descHtmlScrollPane))
        );

        statusTextValue.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        statusTextValue.setForeground(new java.awt.Color(0, 51, 255));
        statusTextValue.setText("Initialized.");
        statusTextValue.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        skipListingBtn.setBackground(new java.awt.Color(255, 51, 51));
        skipListingBtn.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        skipListingBtn.setForeground(new java.awt.Color(255, 255, 255));
        skipListingBtn.setText("Skip Listing");
        skipListingBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        skipListingBtn.setFocusPainted(false);
        skipListingBtn.setFocusable(false);

        publishListingBtn.setBackground(new java.awt.Color(0, 204, 51));
        publishListingBtn.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        publishListingBtn.setForeground(new java.awt.Color(255, 255, 255));
        publishListingBtn.setText("Publish Listing");
        publishListingBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        publishListingBtn.setFocusPainted(false);
        publishListingBtn.setFocusable(false);
        publishListingBtn.setMaximumSize(new java.awt.Dimension(115, 20));
        publishListingBtn.setMinimumSize(new java.awt.Dimension(115, 20));
        publishListingBtn.setPreferredSize(new java.awt.Dimension(115, 20));

        platformDropdown.setBackground(new java.awt.Color(214, 223, 233));
        platformDropdown.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        platformDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "eBay", "Amazon", "Wish" }));
        platformDropdown.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        platformDropdown.setFocusable(false);

        platformLabel.setFont(new java.awt.Font("SansSerif", 1, 11)); // NOI18N
        platformLabel.setText("PLATFORM");

        final javax.swing.GroupLayout statusAreaPanelLayout = new javax.swing.GroupLayout(statusAreaPanel);
        statusAreaPanel.setLayout(statusAreaPanelLayout);
        statusAreaPanelLayout.setHorizontalGroup(
            statusAreaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusAreaPanelLayout.createSequentialGroup()
                .addGroup(statusAreaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(statusAreaPanelLayout.createSequentialGroup()
                        .addComponent(platformLabel)
                        .addGap(18, 18, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusAreaPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(platformDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)))
                .addComponent(skipListingBtn)
                .addGap(0, 0, 0)
                .addComponent(publishListingBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(statusTextValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        statusAreaPanelLayout.setVerticalGroup(
            statusAreaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusAreaPanelLayout.createSequentialGroup()
                .addComponent(statusTextValue, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(statusAreaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(skipListingBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(statusAreaPanelLayout.createSequentialGroup()
                        .addComponent(platformLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(platformDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(publishListingBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        recentSalesHeaderText.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        recentSalesHeaderText.setText("Recent sales");

//        recentSalesScrollPane.setName(""); // NOI18N
//
//        recentSalesPanel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
//
//        final javax.swing.GroupLayout recentSalesPanelLayout = new javax.swing.GroupLayout(recentSalesPanel);
//        recentSalesPanel.setLayout(recentSalesPanelLayout);
//        recentSalesPanelLayout.setHorizontalGroup(
//            recentSalesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//            .addGap(0, 368, Short.MAX_VALUE)
//        );
//        recentSalesPanelLayout.setVerticalGroup(
//            recentSalesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//            .addGap(0, 941, Short.MAX_VALUE)
//        );

//        recentSalesScrollPane.setViewportView(recentSalesPanel);

        soldItemsCheckbox.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        soldItemsCheckbox.setText("Sold items");

//        final javax.swing.GroupLayout recentSalesPanelContainerLayout = new javax.swing.GroupLayout(recentSalesPanelContainer);
//        recentSalesPanelContainer.setLayout(recentSalesPanelContainerLayout);
//        recentSalesPanelContainerLayout.setHorizontalGroup(
//            recentSalesPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//            .addGroup(recentSalesPanelContainerLayout.createSequentialGroup()
//                .addComponent(recentSalesHeaderText)
//                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
//                .addComponent(soldItemsCheckbox))
//            .addComponent(recentSalesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
//        );
//        recentSalesPanelContainerLayout.setVerticalGroup(
//            recentSalesPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//            .addGroup(recentSalesPanelContainerLayout.createSequentialGroup()
//                .addGroup(recentSalesPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
//                    .addComponent(recentSalesHeaderText)
//                    .addComponent(soldItemsCheckbox, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
//                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
//                .addComponent(recentSalesScrollPane))
//        );

        imagesPanelHeaderText.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        imagesPanelHeaderText.setText("Images");

        imagesPanelScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        imagesPanel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
        imagesPanelScrollPane.setViewportView(imagesPanel);

        final javax.swing.GroupLayout imagesPanelContainerLayout = new javax.swing.GroupLayout(imagesPanelContainer);
        imagesPanelContainer.setLayout(imagesPanelContainerLayout);
        imagesPanelContainerLayout.setHorizontalGroup(
            imagesPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagesPanelContainerLayout.createSequentialGroup()
                .addComponent(imagesPanelHeaderText)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(imagesPanelScrollPane)
        );
        imagesPanelContainerLayout.setVerticalGroup(
            imagesPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagesPanelContainerLayout.createSequentialGroup()
                .addComponent(imagesPanelHeaderText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imagesPanelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
        );

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(imagesPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(fulfillmentsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(statusAreaPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(generalInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(recentSalesPanelContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(generalInfoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(recentSalesPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fulfillmentsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusAreaPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imagesPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JTextField brandInput;
    private javax.swing.JLabel brandLabel;
    public javax.swing.JComboBox categoryDropdown;
    public javax.swing.JLabel categoryLabel;
    public javax.swing.JScrollPane descHtmlScrollPane;
    public javax.swing.JEditorPane descHtmlView;
    public javax.swing.JEditorPane descRawInput;
    public javax.swing.JScrollPane descRawScrollPane;
    public javax.swing.JLabel descriptionLabel;
    public javax.swing.JPanel fulfillmentsPanel;
    public javax.swing.JLabel fulfillmentsPanelAddLabelText;
    public javax.swing.JLabel fulfillmentsPanelHeaderText;
    public javax.swing.JTextField fulfillmentsPanelInput;
    public javax.swing.JButton fullfillmentsPanelFileBtn;
    public javax.swing.JPanel generalInfoPanel;
    public javax.swing.JLabel generalInfoPanelHeaderText;
    public javax.swing.JPanel imagesPanel;
    public javax.swing.JPanel imagesPanelContainer;
    public javax.swing.JLabel imagesPanelHeaderText;
    public javax.swing.JScrollPane imagesPanelScrollPane;
    public javax.swing.JTextField listingPriceInput;
    public javax.swing.JLabel listingPriceLabel;
    public javax.swing.JTextField listingTitleInput;
    public javax.swing.JLabel listingTitleLabel;
    public javax.swing.JLabel parsedQueueLabel;
    public javax.swing.JLabel parsedQueueValue;
    public javax.swing.JComboBox platformDropdown;
    public javax.swing.JLabel platformLabel;
    public javax.swing.JTextField profitMarginInput;
    public javax.swing.JLabel profitMarginLabel;
    public javax.swing.JButton publishListingBtn;
    public javax.swing.JPanel queuePanelContainer;
    public javax.swing.JLabel recentSalesHeaderText;
//    public javax.swing.JPanel recentSalesPanel;
    public javax.swing.JPanel recentSalesPanelContainer;
//    public javax.swing.JScrollPane recentSalesScrollPane;
    public javax.swing.JButton resetListingInformation;
    public javax.swing.JTextField shippingPriceInput;
    public javax.swing.JLabel shippingPriceLabel;
    public javax.swing.JButton skipListingBtn;
    public javax.swing.JCheckBox soldItemsCheckbox;
    public JButton shuffleUrlQueue;
    public javax.swing.JPanel statusAreaPanel;
    public javax.swing.JLabel statusTextValue;
    public javax.swing.JLabel urlsToParseLabel;
    public javax.swing.JLabel urlsToParseValue;
    // End of variables declaration//GEN-END:variables
}
