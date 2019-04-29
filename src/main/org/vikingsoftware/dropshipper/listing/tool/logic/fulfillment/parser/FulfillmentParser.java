package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public interface FulfillmentParser<T extends WebDriver> {

	public Listing getListingTemplate(final FulfillmentAccount account, final String url);
	public Class<? extends DriverSupplier<T>> getDriverSupplierClass();
	public boolean needsToLogin();

}
