package main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics.model;

import java.util.Optional;
import java.util.function.Function;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public enum RequiredItemSpecific {
	BRAND("Brand", true, listing -> listing.brand),
	PRODUCT("Product", listing -> listing.category.name),
	MPN("MPN", listing -> listing.mpn);
	
	public final String name;
	public final boolean isProvidedByOtherGUIElement;
	public final Function<Listing, String> preFilledValueFunction;
	
	RequiredItemSpecific(final String name, final Function<Listing, String> preFilledValueFunction) {
		this.name = name;
		this.isProvidedByOtherGUIElement = false;
		this.preFilledValueFunction = preFilledValueFunction;
	}
	
	RequiredItemSpecific(final String name, final boolean isProvidedByOtherGUIElement, final Function<Listing, String> preFilledValueFunction) {
		this.name = name;
		this.isProvidedByOtherGUIElement = isProvidedByOtherGUIElement;
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
}
