package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import org.openqa.selenium.By;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class SamsClubWebDriver extends LoginWebDriver {

	@Override
	public boolean selectOrderOptions(SkuMapping skuMapping, FulfillmentListing listing) {
		return false;
	}

	@Override
	protected boolean prepareForExecutionViaLoginImpl() {
		try {
			get("https://www.samsclub.com/sams/account/signin/login.jsp");

			System.out.println("Logging in with account: " + account.username);
			System.out.println("Entering username");
			findElement(By.id("email")).sendKeys(account.username);
			System.out.println("Entering password");
			findElement(By.id("password")).sendKeys(account.password);
			findElement(By.className("sc-btn-primary")).click();
			
			System.out.println("Logging in...");
			get("https://www.samsclub.com/account/summary");
			System.out.println("Verifying log in was successful...");
			//verify we logged in successfully
			findElement(By.className("sc-account-member-membership-title"));
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected String getLandingPageURL() {
		return "https://www.samsclub.com";
	}

	@Override
	protected boolean verifyLoggedIn() {
		try {
			get("https://www.samsclub.com/account");

			//verify we logged in successfully
			findElement(By.className("sc-account-member-membership-title"));
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
