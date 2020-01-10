package main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics.model.RequiredItemSpecific;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics.model.RequiredItemSpecific.ItemSpecificType;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public class ItemSpecificsPanel extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private final BorderLayout borderLayout;
	private final GridLayout gridLayout;
	private final JScrollPane scrollPane;
	private final JPanel specificsPanel = new JPanel();
	private final Map<String, JComboBox<String>> specificToComboBoxCache = new HashMap<>();
	private final Listing listing;
	private final AtomicBoolean finished;
	
	public ItemSpecificsPanel(final Listing listing, final AtomicBoolean finished) {
		this.listing = listing;
		this.finished = finished;
		this.gridLayout = new GridLayout(listing.requiredItemSpecifics.size(), 2);
		this.gridLayout.setHgap(10);
		this.borderLayout = new BorderLayout();
		this.scrollPane = new JScrollPane(specificsPanel);
		this.setLayout(borderLayout);
		this.specificsPanel.setLayout(gridLayout);
		if(addItemSpecificFields(listing)) {
			this.add(new JLabel("Item Specifics Panel"), BorderLayout.NORTH);
			this.add(scrollPane, BorderLayout.CENTER);
			final JButton submitButton = new JButton("Submit");
			submitButton.addActionListener(this::submit);
			this.add(submitButton, BorderLayout.SOUTH);
			this.pack();
			this.setVisible(true);
			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					finished.set(true);
					super.windowClosed(e);
				}
			});
		} else {
			finished.set(true);
		}
	}
	
	private boolean addItemSpecificFields(final Listing listing) {
		boolean addedToPanel = false;
		final Map<String, List<String>> requiredSpecs = listing.requiredItemSpecifics;
		for(final String itemSpecificName : requiredSpecs.keySet()) {
			final List<String> recommendedOptions = requiredSpecs.get(itemSpecificName);
			String preFilledValue = null;
			final Optional<RequiredItemSpecific> requiredItemSpec = RequiredItemSpecific.getRequiredItemSpecific(itemSpecificName);
			if(requiredItemSpec.isPresent()) {
				if(requiredItemSpec.get().type != ItemSpecificType.NEEDS_MANUAL_SELECTION) {
					listing.itemSpecifics.put(requiredItemSpec.get().name, requiredItemSpec.get().preFilledValueFunction.apply(listing));
					continue;
				}
				addedToPanel = true;
				if(requiredItemSpec.get().preFilledValueFunction != null) {
					preFilledValue = requiredItemSpec.get().preFilledValueFunction.apply(listing);
					if(preFilledValue != null) {
						recommendedOptions.add(preFilledValue);
					}
				}
			}
			
			final JComboBox<String> comboBox = new JComboBox<>(recommendedOptions.toArray(new String[recommendedOptions.size()]));
			if(preFilledValue != null) {
				comboBox.setSelectedItem(preFilledValue);
			}
			specificToComboBoxCache.put(itemSpecificName, comboBox);
			this.specificsPanel.add(new JLabel(itemSpecificName));
			this.specificsPanel.add(comboBox);
		}
		
		return addedToPanel;
	}
	
	private void submit(final ActionEvent e) {
		
		for(final Map.Entry<String, JComboBox<String>> entry : specificToComboBoxCache.entrySet()) {
			listing.itemSpecifics.put(entry.getKey(), entry.getValue().getSelectedItem().toString());
		}
		
		this.dispose();
		finished.set(true);
	}

}
