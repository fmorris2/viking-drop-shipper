package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser;

import javax.swing.SwingUtilities;

import org.openqa.selenium.TimeoutException;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public abstract class AbstractFulfillmentParser<T extends LoginWebDriver> implements FulfillmentParser<T> {

	protected T driver;

	protected abstract Listing parseListing();

	@Override
	public Listing getListingTemplate(final FulfillmentAccount account, final String url) {
		DriverSupplier<T> supplier = null;

		try {
			supplier = BrowserRepository.get().request(getDriverSupplierClass());
			driver = supplier.get();
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Preparing web driver"));
			if(!needsToLogin() || driver.getReady(account)) {
				SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Loading Fulfillment Listing URL"));
				driver.get(url);
				return parseListing();
			}
		} catch(final TimeoutException e) {
			e.printStackTrace();
			BrowserRepository.get().replace(supplier);
			supplier = null;
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			if(supplier != null) {
				BrowserRepository.get().relinquish(supplier);
			}
		}
		return null;
	}

}
