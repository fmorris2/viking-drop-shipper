package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class SamsClubWebDriver extends LoginWebDriver {
	
	private static final int MAX_LOGIN_ATTEMPTS = 2;
	
	private int loginAttempts = 0;
	
	private WebElement usernameEl, passwordEl, buttonEl;
	
	@Override
	public boolean selectOrderOptions(SkuMapping skuMapping, FulfillmentListing listing) {
		return false;
	}

	@Override
	protected boolean prepareForExecutionViaLoginImpl() {
		try {
			usernameEl = null;
			passwordEl = null;
			buttonEl = null;
			
			if(loginAttempts++ > MAX_LOGIN_ATTEMPTS) {
				return false;
			}

			loginAttempts++;
			get("https://www.samsclub.com/sams/account/signin/login.jsp");

			System.out.println("Logging in with account: " + account.username);
			savePageSource("sams-login-page.html");
			
			this.setImplicitWait(1);
			findUsernameEl();
			findPasswordEl();
			findButtonEl();
			this.resetImplicitWait();
			
			if(usernameEl != null && passwordEl != null && buttonEl != null) {
				System.out.println("Logging in... " + getCurrentUrl());
				usernameEl.click();
				sendKeysSlowly(usernameEl, account.username);
				sleep(1500);
				passwordEl.click();
				passwordEl.sendKeys(account.password);
				sleep(1500);
				buttonEl.click();
				sleep(4000);
				
				return true;
			}
			
			return false;
		} catch(final Exception e) {
			e.printStackTrace();
			return prepareForExecutionViaLoginImpl();
		} finally {
			loginAttempts = 0;
		}
	}
	
	private void findUsernameEl() {
		System.out.println("Finding username el");
		try {
			usernameEl = findElementNormal(By.id("email"));
		} catch (Exception e) {
			usernameEl = findElementNormal(By.id("txtLoginEmailID"));
		}
		System.out.println("Found username el.");
	}
	
	private void findPasswordEl() {
		System.out.println("Finding password el");
		try {
			passwordEl = findElementNormal(By.id("password"));
		} catch (final Exception e) {
			passwordEl = findElementNormal(By.id("txtLoginPwd"));
		}
		System.out.println("Found password el.");
	}
	
	private void findButtonEl() {
		System.out.println("Finding button el");
		try {
			buttonEl = findElementNormal(By.className("sc-btn-primary"));
		} catch(final Exception e) {
			buttonEl = findElementNormal(By.cssSelector("#signInButton"));
		}
		System.out.println("Found button el.");
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
