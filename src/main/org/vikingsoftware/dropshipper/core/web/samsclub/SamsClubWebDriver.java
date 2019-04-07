package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.UnableToSetCookieException;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class SamsClubWebDriver extends LoginWebDriver {

	private static final Map<FulfillmentAccount, Set<Cookie>> sessionCookies = new HashMap<>();
	private static final Map<SamsClubWebDriver, Set<Cookie>> cookieCache = new HashMap<>();
	private static final Map<FulfillmentAccount, Object> loginLocks = new HashMap<>();

	private FulfillmentAccount account;

	@Override
	public boolean getReady(final FulfillmentAccount account) {

		this.account = account;
		manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
		manage().window().maximize();

		final Object loginLock = loginLocks.computeIfAbsent(account, acc -> new Object());
		synchronized(loginLock) {
			if(sessionCookies.computeIfAbsent(account, acc -> new HashSet<>()).isEmpty()) {
				System.out.println("prepareForExecution");
				return prepareForExecutionViaLogin();
			}
		}

		System.out.println("prepareWithPreExistingSession");
		return prepareWithPreExistingSession();
	}

	public void clearSession() {
		sessionCookies.put(account, new HashSet<>());
	}

	private boolean prepareForExecutionViaLogin() {
		try {
			if(!sessionCookies.computeIfAbsent(account, acc -> new HashSet<>()).isEmpty()) {
				return prepareWithPreExistingSession();
			}
			get("https://www.samsclub.com/sams/account/signin/login.jsp");

			findElement(By.id("txtLoginEmailID")).sendKeys(account.username);
			findElement(By.id("txtLoginPwd")).sendKeys(account.password);
			findElement(By.id("signInButton")).click();

			//verify we logged in successfully
			findElement(By.className("sc-account-member-membership-title"));
			final Set<Cookie> cookies = new HashSet<>();
			cookies.addAll(manage().getCookies());
			sessionCookies.put(account, cookies);
			return true;

		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.high(getClass(), "failed to prepare for execution: ", e);
		}

		return false;
	}

	private boolean prepareWithPreExistingSession() {
		try {
			System.out.println("Checking if " + this + " already has pre existing cookies...");
			final Set<Cookie> sessionCooks = sessionCookies.computeIfAbsent(account, acc -> new HashSet<>());
			if(cookieCache.computeIfAbsent(this, driver -> new HashSet<>()) != sessionCooks) {

				//web driver spec says we need to land on the page first before setting cookies
				get("https://www.samsclub.com");

				System.out.println(this + " needs to add pre existing session cookies!");
				manage().deleteAllCookies();
				for(final Cookie cookie : sessionCooks) {
					try {
						manage().addCookie(cookie);
					} catch(final UnableToSetCookieException e) {
						System.out.println("Unable to set cookie: " + cookie);
					}
				}

				cookieCache.put(this, sessionCooks);
				System.out.println(this + " is done adding pre existing session cookies");

				get("https://www.samsclub.com/account");

				//verify we logged in successfully
				findElement(By.className("sc-account-member-membership-title"));

			} else {
				System.out.println(this + " HAS PRE EXISTING COOKIES!");
			}

			return true;
		} catch(final Exception e) {
			e.printStackTrace();
			DBLogging.high(getClass(), "failed to prepare for execution using pre existing session: ", e);
		}

		return false;
	}

}
