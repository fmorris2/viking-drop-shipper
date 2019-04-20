package main.org.vikingsoftware.dropshipper.core.web.costco;

import org.openqa.selenium.By;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class CostcoWebDriver extends LoginWebDriver {

	@Override
	public boolean selectOrderOptions(SkuMapping skuMapping, FulfillmentListing listing) {
		return false;
	}

	@Override
	protected boolean prepareForExecutionViaLoginImpl() {
		try {
			get("https://www.costco.com/LogonForm");

			System.out.println("Logging in with account: " + account.username);
			findElement(By.id("logonId")).sendKeys(account.username);
			findElement(By.id("logonPassword")).sendKeys(account.password);
			findElement(By.cssSelector("#LogonForm input[type=\"submit\"]")).click();
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected String getLandingPageURL() {
		return "http://costco.com";
	}

	@Override
	protected boolean verifyLoggedIn() {
		try {
			get("https://www.costco.com/AccountInformationView?identifier=manage-membership");

			final String emailValue = findElement(By.id("email-value")).getText();
			return emailValue.equalsIgnoreCase(account.username);
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
