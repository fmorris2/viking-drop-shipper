package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.util.Collections;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;

public final class ListingQueue {

	private static final LinkedList<Listing> queue = new LinkedList<>();

	public static int size() {
		return queue.size();
	}

	public static void replaceFirst(final Listing listing) {
		queue.poll();
		queue.addFirst(listing);
	}

	public static void add(final Listing listing) {
		queue.add(listing);
		updateQueueSizeLabel();
	}

	public static Listing peek() {
		return queue.peek();
	}

	public static Listing poll() {
		final Listing listing = queue.poll();
		updateQueueSizeLabel();
		return listing;
	}
	
	public static void shuffle() {
		Collections.shuffle(queue);
	}

	public static boolean isEmpty() {
		return queue.isEmpty();
	}

	private static void updateQueueSizeLabel() {
		SwingUtilities.invokeLater(() -> ListingToolGUI.get().parsedQueueValue.setText(Integer.toString(queue.size())));
	}
}
