package main.org.vikingsoftware.dropshipper.listing.tool.logic;

import java.awt.image.BufferedImage;

import main.org.vikingsoftware.dropshipper.core.utils.ImageUtils;

public class ListingImage {
	public final String url;
	
	private BufferedImage image;

	public ListingImage(final String url) {
		this.url = url;
	}
	
	public BufferedImage getImage() {
		if(image == null) {
			image = ImageUtils.getImageFromUrl(url);
		}
		
		return image;
	}
}
