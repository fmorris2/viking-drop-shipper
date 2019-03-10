package main.org.vikingsoftware.dropshipper.listing.tool.logic;

public class PropertyItemOption {
	public String skuId;
	public String elementId;
	public String title;
	public String thumbNailUrl;
	public String fullImageUrl;
	
	@Override
	public String toString() {
		return title;
//		return "skuId: " + skuId + "\n"
//			+  "elementId: " + elementId + "\n"
//			+  "title: " + title + "\n"
//			+  "thumbNailUrl: " + thumbNailUrl + "\n"
//			+  "fullImageUrl: " + fullImageUrl;
	}
}
