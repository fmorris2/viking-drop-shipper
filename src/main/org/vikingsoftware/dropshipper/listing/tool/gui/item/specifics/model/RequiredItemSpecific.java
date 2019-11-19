package main.org.vikingsoftware.dropshipper.listing.tool.gui.item.specifics.model;

import java.util.Optional;
import java.util.function.Function;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public enum RequiredItemSpecific {
	BRAND("Brand", listing -> listing.brand),
	PRODUCT("Product", null);
	
	public final String name;
	public final Function<Listing, Object> preFilledValueFunction;
	
	RequiredItemSpecific(final String name, final Function<Listing, Object> preFilledValueFunction) {
		this.name = name;
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
