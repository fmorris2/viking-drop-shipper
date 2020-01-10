package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;

public final class ListingQueue {

	private static final LinkedList<Listing> queue = new LinkedList<>();
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public static int size() {
		lock.readLock().lock();
		try {
			return queue.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	public static void replaceFirst(final Listing listing) {
		lock.writeLock().lock();
		try {
			queue.poll();
			queue.addFirst(listing);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static void add(final Listing listing) {
		lock.writeLock().lock();
		try {
			queue.add(listing);
			updateQueueSizeLabel();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static Listing peek() {
		lock.readLock().lock();
		try {
			return queue.peek();
		} finally {
			lock.readLock().unlock();
		}
	}

	public static Listing poll() {
		lock.writeLock().lock();
		try {
			final Listing listing = queue.poll();
			updateQueueSizeLabel();
			return listing;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public static void shuffle() {
		lock.writeLock().lock();
		try {
			Collections.shuffle(queue);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public static boolean isEmpty() {
		lock.readLock().lock();
		try {
			return queue.isEmpty();
		} finally {
			lock.readLock().unlock();
		}
	}

	private static void updateQueueSizeLabel() {
		SwingUtilities.invokeLater(() -> ListingToolGUI.get().parsedQueueValue.setText(Integer.toString(queue.size())));
	}
}
