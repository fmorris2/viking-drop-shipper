package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import javax.swing.SwingUtilities;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public abstract class AbstractFulfillmentParser<T extends LoginWebDriver> implements FulfillmentParser<T> {

	protected T driver;

	protected abstract Listing parseListing(final String url);

	@Override
	public Listing getListingTemplate(final FulfillmentAccount account, final String url) {
		final Class<?> driverSupplierClass = getDriverSupplierClass();
		
		if(driverSupplierClass == null) {
			return handleParsing(url);
		}
		
		DriverSupplier<T> supplier = null;

		try {
			supplier = BrowserRepository.get().request(driverSupplierClass);
			driver = supplier.get();
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Preparing web driver"));
			if(!needsToLogin() || driver.getReady(account)) {
				System.out.println("Navigating to listing URL: " + url);
				SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Loading Fulfillment Listing URL"));
				driver.get(url);
				System.out.println("About to parse listing in AbstractFulfillmentParser");
				return handleParsing(url);
			}
		} catch(final Exception e) {
			supplier = null;
			e.printStackTrace();
		} finally {
			if(supplier != null) {
				BrowserRepository.get().relinquish(supplier);
			} else {
				BrowserRepository.get().replace(this.getDriverSupplierClass());
			}
		}
		return null;
	}
	
	private Listing handleParsing(final String url) {
		final Listing listing = parseListing(url);
		if(listing != null) {
			listing.url = url;
			return listing;
		}
		
		return null;
	}

}
