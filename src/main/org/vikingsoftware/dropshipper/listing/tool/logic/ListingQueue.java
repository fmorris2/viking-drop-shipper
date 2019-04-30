package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;

public final class ListingQueue {

	private static final Queue<Listing> queue = new LinkedList<>();

	public static int size() {
		return queue.size();
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

	public static boolean isEmpty() {
		return queue.isEmpty();
	}

	private static void updateQueueSizeLabel() {
		SwingUtilities.invokeLater(() -> ListingToolGUI.get().queueSizeValue.setText(Integer.toString(queue.size())));
	}
}
