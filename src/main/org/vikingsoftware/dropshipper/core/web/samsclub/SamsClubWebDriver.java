package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.openqa.selenium.By;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class SamsClubWebDriver extends LoginWebDriver {
	
	private static final int MAX_LOGIN_ATTEMPTS = 2;
	
	private int loginAttempts = 0;
	
	@Override
	public boolean selectOrderOptions(SkuMapping skuMapping, FulfillmentListing listing) {
		return false;
	}

	@Override
	protected boolean prepareForExecutionViaLoginImpl() {
		try {
			if(loginAttempts++ > MAX_LOGIN_ATTEMPTS) {
				return false;
			}

			loginAttempts++;
			get("https://www.samsclub.com/sams/account/signin/login.jsp");

			System.out.println("Logging in with account: " + account.username);
			System.out.println("Entering username");
			setImplicitWait(1);
			try {
				findElement(By.id("email")).sendKeys(account.username);
			} catch (Exception e) {
				findElement(By.id("txtLoginEmailID")).sendKeys(account.username);
			}
			
			System.out.println("Entering password");
			try {
				findElement(By.id("password")).sendKeys(account.password);
			} catch (final Exception e) {
				findElement(By.id("txtLoginPwd")).sendKeys(account.password);
			}
			
			System.out.println("Logging in... " + getCurrentUrl());
			try {
				findElement(By.cssSelector("button.sc-primary-button:nth-child(4)")).click();
			} catch(final Exception e) {
				findElement(By.cssSelector("#signInButton")).click();
			}
			
			sleep(4000);
			return verifyLoggedIn();
		} catch(final Exception e) {
			e.printStackTrace();
			return prepareForExecutionViaLoginImpl();
		} finally {
			loginAttempts = 0;
		}
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
