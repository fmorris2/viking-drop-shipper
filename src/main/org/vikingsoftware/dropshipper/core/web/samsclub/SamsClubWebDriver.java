package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.UnableToSetCookieException;

import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class SamsClubWebDriver extends LoginWebDriver {

	private static final Set<Cookie> sessionCookies = new HashSet<>();

	private static Object loginLock = new Object();

	@Override
	public boolean getReady() {

		manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);

		if(sessionCookies.isEmpty()) {
			System.out.println("prepareForExecution");
			return prepareForExecutionViaLogin();
		}

		System.out.println("prepareWithPreExistingSession");
		return prepareWithPreExistingSession();
	}

	@Override
	protected String getCredsFilePath() {
		return "/data/samsclub-creds-secure";
	}

	public static void clearSession() {
		sessionCookies.clear();
	}

	private boolean prepareForExecutionViaLogin() {
		try {
			synchronized(loginLock) {
				if(!sessionCookies.isEmpty()) {
					return prepareWithPreExistingSession();
				}
				get("https://www.samsclub.com/sams/account/signin/login.jsp");

				findElement(By.id("txtLoginEmailID")).sendKeys(username);
				findElement(By.id("txtLoginPwd")).sendKeys(password);
				findElement(By.id("signInButton")).click();

				//verify we logged in successfully
				findElement(By.className("sc-account-member-membership-title"));
				sessionCookies.clear();
				sessionCookies.addAll(manage().getCookies());
				return true;
			}

		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.high(getClass(), "failed to prepare for execution: ", e);
		}

		return false;
	}

	private boolean prepareWithPreExistingSession() {
		try {
			//web driver spec says we need to land on the page first before setting cookies
			get("https://www.samsclub.com");

			if(!manage().getCookies().equals(sessionCookies)) {
				for(final Cookie cookie : sessionCookies) {
					try {
						manage().addCookie(cookie);
					} catch(final UnableToSetCookieException e) {
						System.out.println("Unable to set cookie: " + cookie);
					}
				}
			}

			get("https://www.samsclub.com/account");

			//verify we logged in successfully
			findElement(By.className("sc-account-member-membership-title"));
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.high(getClass(), "failed to prepare for execution using pre existing session: ", e);
		}

		return false;
	}

}
