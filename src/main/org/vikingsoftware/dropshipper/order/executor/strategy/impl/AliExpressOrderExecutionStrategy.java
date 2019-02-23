package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import main.org.vikingsoftware.dropshipper.core.data.CustomerOrder;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class AliExpressOrderExecutionStrategy implements OrderExecutionStrategy {
	
	private static final String CREDS_FILE_PATH = "/data/aliexpress-creds.secure";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	
	private String username;
	private String password;
	
	public AliExpressOrderExecutionStrategy() {
		parseCredentials();
	}
	
	private void parseCredentials() {
		try(
				final InputStream inputStream = getClass().getResourceAsStream(CREDS_FILE_PATH);
				final InputStreamReader reader = new InputStreamReader(inputStream);
				final BufferedReader bR = new BufferedReader(reader);
			) {
				username = bR.readLine().trim();
				password = bR.readLine().trim();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean order(final CustomerOrder order) {
		return order(order, false);
	}
	
	public boolean testOrder(final CustomerOrder order) {
		return order(order, true);
	}
	
	private boolean order(final CustomerOrder order, final boolean isTest) {
		
		final boolean success = false;
		final FulfillmentListing listing = loadFulfillmentListingForOrder(order);
				
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(false);
		
		final WebDriver browser = new ChromeDriver(options);
		browser.manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
		
		try {
			//sign in to ali express
			browser.get("https://login.aliexpress.com/");
			browser.switchTo().frame(browser.findElement(By.id("alibaba-login-box")));
			browser.findElement(By.id("fm-login-id")).sendKeys(username);
			browser.findElement(By.id("fm-login-password")).sendKeys(password);
			browser.findElement(By.id("fm-login-submit")).click();
			
			//wait for home page to appear
			browser.findElement(By.className("nav-user-account"));		
			
			
			Thread.sleep(5000);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			//browser.close();
		}
		
		return success;
	}
	
	private FulfillmentListing loadFulfillmentListingForOrder(final CustomerOrder order) {
		return null;
	}
}
