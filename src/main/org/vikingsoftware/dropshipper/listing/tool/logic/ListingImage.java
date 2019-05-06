package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.awt.image.BufferedImage;

public class ListingImage {
	public final String url;
	public final BufferedImage image;

	public ListingImage(final String url, final BufferedImage img) {
		this.url = url;
		this.image = img;
	}
}
