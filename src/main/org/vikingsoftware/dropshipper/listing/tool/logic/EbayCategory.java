package main.org.vikingsoftware.dropshipper.listing.tool.logic;

public class EbayCategory {
	public final String id;
	public final String name;

	public EbayCategory(final String id, final String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
