package main.org.vikingsoftware.dropshipper.core.web.costco;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

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
			findElement(By.id("LogonForm")); //waits for logon form to be visible
			System.out.println("Logging in with account: " + account.username);

			js("document.getElementById('logonId').value='"+account.username+"';");
			js("document.getElementById('logonPassword').value='"+account.password+"';");
			getKeyboard().sendKeys(Keys.ESCAPE);
			findElement(By.id("LogonForm")).click();
			js("document.getElementById('LogonForm').submit();");
			System.out.println("Submitted logon form!");
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
		final long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS * 1000) {
			try {
				final String cookieString = (String) js("return document.cookie");
				if(cookieString.contains("rememberedLogonId")) {
					return true;
				}

				Thread.sleep(100);
			} catch(final Exception e) {
				//swallow
			}
		}

		return false;
	}
}
