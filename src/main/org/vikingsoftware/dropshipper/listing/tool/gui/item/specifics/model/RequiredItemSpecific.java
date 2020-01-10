package main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics.model;

import java.util.Optional;
import java.util.function.Function;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public enum RequiredItemSpecific {
	BRAND("Brand", ItemSpecificType.PROVIDED_BY_GUI, listing -> listing.brand),
	PRODUCT("Product", listing -> listing.category.name),
	MPN("MPN", ItemSpecificType.PROVIDED_BY_LISTING, listing -> listing.mpn),
	UPC("UPC", ItemSpecificType.PROVIDED_BY_LISTING, listing -> listing.upc);
	
	public final String name;
	public final ItemSpecificType type;
	public final Function<Listing, String> preFilledValueFunction;
	
	RequiredItemSpecific(final String name, final Function<Listing, String> preFilledValueFunction) {
		this.name = name;
		this.type = ItemSpecificType.NEEDS_MANUAL_SELECTION;
		this.preFilledValueFunction = preFilledValueFunction;
	}
	
	RequiredItemSpecific(final String name, final ItemSpecificType type, final Function<Listing, String> preFilledValueFunction) {
		this.name = name;
		this.type = type;
		this.preFilledValueFunction = preFilledValueFunction;
	}
	
	public static Optional<RequiredItemSpecific> getRequiredItemSpecific(final String name) {
		for(final RequiredItemSpecific specific : values()) {
			if(specific.name.equalsIgnoreCase(name)) {
				return Optional.of(specific);
			}
		}
		
		return Optional.empty();
	}
	
	public enum ItemSpecificType {
		PROVIDED_BY_GUI,
		PROVIDED_BY_LISTING,
		NEEDS_MANUAL_SELECTION;
	}
}
