package main.org.vikingsoftware.dropshipper.core.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;


public class AliExpressWebDriver extends LoginWebDriver {
	
	private static final String CREDS_FILE_PATH = "/data/aliexpress-creds.secure";
	private static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 5;
	private static final int MAX_LOGIN_TRIES = 20;
	
	private String username;
	private String password;
	private int loginTries = 0;
	
	public AliExpressWebDriver() {
		super();
		parseCredentials();
	}
	
	@Override
	public boolean getReady() {
		return prepareForExecution();
	}
	
	private boolean prepareForExecution() {
		try {
			manage().window().maximize();
			manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			
			//sign in to ali express
			get("https://login.aliexpress.com/");
			switchTo().frame(findElement(By.id("alibaba-login-box")));
			
			findElement(By.id("fm-login-id")).sendKeys(username);
			findElement(By.id("fm-login-password")).sendKeys(password);
			findElement(By.id("fm-login-submit")).click();

			try {
				//wait for home page to appear
				findElement(By.className("nav-user-account"));
			} catch(final NoSuchElementException e) {
				if(loginTries < MAX_LOGIN_TRIES) {
					System.out.println("encountered verification element... retrying.");
					close();
					loginTries++;
					return prepareForExecution();
				} else {
					return false;
				}
			}
			
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		close();
		return false;
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
	
}
