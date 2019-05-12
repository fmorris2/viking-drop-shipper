package main.org.vikingsoftware.dropshipper.core.web.costco;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

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
			final String displayVal = ((JavascriptExecutor)this).executeScript("return document.getElementById('header_sign_in').style.display").toString();
			return displayVal.equalsIgnoreCase("none");
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
