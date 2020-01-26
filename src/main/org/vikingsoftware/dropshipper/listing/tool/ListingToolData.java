package main.org.vikingsoftware.dropshipper.listing.tool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public class ListingToolData {
	
	private static ListingToolData instance;
	
	private final Queue<Listing> listingQueue = new ConcurrentLinkedQueue<>();
	
	private Listing currentListing;
	
	private ListingToolData() {
		
	}
	
	public static ListingToolData get() {
		if(instance == null) { 
			instance = new ListingToolData();
		}
		
		return instance;
	}
	
	public void addListingToQueue(final Listing listing) {
		if(listing != null) {
			listingQueue.add(listing);
		}
	}
	
	public Listing nextListing() {
		currentListing = listingQueue.poll();
		return currentListing;
	}
	
	public Listing getCurrentListing() {
		return currentListing;
	}
	
	
	
}
