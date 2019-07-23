package main.org.vikingsoftware.dropshipper.core.web;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.UnableToSetCookieException;
import org.openqa.selenium.WebElement;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;

public abstract class LoginWebDriver extends JBrowserDriver {

	public static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 20;

	protected static final int MAX_LOGIN_TRIES = 20;
	
	private static final int DEFAULT_TIMEOUT_MS = 30_000;

	private static final Map<FulfillmentAccount, Set<Cookie>> sessionCookies = new ConcurrentHashMap<>();
	private static final Map<LoginWebDriver, Set<Cookie>> cookieCache = new ConcurrentHashMap<>();
	private static final Map<FulfillmentAccount, Object> loginLocks = new ConcurrentHashMap<>();

	protected final Map<String,String> cachedOrderOptions = new HashMap<>();

	protected int loginTries = 0;
	protected FulfillmentAccount account;

	public abstract boolean selectOrderOptions(final SkuMapping skuMapping, final FulfillmentListing listing);

	protected abstract boolean prepareForExecutionViaLoginImpl();
	protected abstract String getLandingPageURL();
	protected abstract boolean verifyLoggedIn();

	public LoginWebDriver() {
		super(getSettingsBuilder().build()
		);
	}
	
	public static Settings.Builder getSettingsBuilder() {
		return new Settings.Builder()
			.headless(false)
			.loggerLevel(Level.SEVERE)
			.connectionReqTimeout(DEFAULT_TIMEOUT_MS)
			.connectTimeout(DEFAULT_TIMEOUT_MS);
	}
	
	public void savePageSource() {
		try(final FileWriter fW = new FileWriter("current-page-source");
				final BufferedWriter bW = new BufferedWriter(fW);) {
			bW.write(getPageSource());
		} catch(final Exception e) {}
	}

	public boolean getReady(final FulfillmentAccount account) {
		System.out.println("LoginWebDriver#getReady");
		this.account = account;
		try {
			manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
			manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
			manage().window().maximize();
			
			if(account == null) {
				System.out.println("account is null: " + account);
			}
			final Object loginLock = loginLocks.computeIfAbsent(account, acc -> new Object());
			synchronized(loginLock) {
				if(sessionCookies.computeIfAbsent(account, acc -> new HashSet<>()).isEmpty()) {
					System.out.println("prepareForExecution");
					return prepareForExecutionViaLogin();
				}
			}

			System.out.println("prepareWithPreExistingSession");
			return prepareWithPreExistingSession();
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public WebElement findElement(By by) {
		final long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < DEFAULT_VISIBILITY_WAIT_SECONDS * 1000) {
			try {
				return super.findElement(by);
			} catch(final Exception e) {
				//swallow
			}
		}

		return null;
	}

	public Object js(final String command) {
		return ((JavascriptExecutor)this).executeScript(command);
	}

	public void resetImplicitWait() {
		manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
	}

	public void setImplicitWait(final int seconds) {
		manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
	}

	public void clearSession() {
		sessionCookies.put(account, new HashSet<>());
	}

	public void clearCachedSelectedOrderOptions() {
		cachedOrderOptions.clear();
	}

	public void scrollIntoView(final WebElement el) {
		final JavascriptExecutor jse = this;
		jse.executeScript("arguments[0].scrollIntoView()", el);
	}

	public String waitForTextToAppear(final Supplier<WebElement> element, final long ms) {
		final long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < ms) {
			final WebElement el = element.get();
			String txt = el.getText();
			if(txt != null && !txt.isEmpty()) {
				return txt;
			} else if((txt = el.getAttribute("value")) != null && !txt.isEmpty()) {
				return txt;
			}
		}

		return null;
	}

	public void sendKeysSlowly(final WebElement el, final String keys) throws InterruptedException {
		final char[] chars = keys.toCharArray();
		for(final char character : chars) {
			el.sendKeys(""+character);
			Thread.sleep(2);
		}
	}
	
	public void sleep(final long ms) {
		try {
			Thread.sleep(ms);
		} catch(final InterruptedException e) {}
	}

	public void saveCurrentPageToFile(final String fileName) {
		final String pageSource = getPageSource();

		try(final FileWriter fR = new FileWriter(fileName + ".html");
			final BufferedWriter bR = new BufferedWriter(fR)) {
			bR.write(pageSource);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private boolean prepareForExecutionViaLogin() {
		try {
			if(!sessionCookies.computeIfAbsent(account, acc -> new HashSet<>()).isEmpty()) {
				return prepareWithPreExistingSession();
			}
			manage().deleteAllCookies();
			final boolean prepared = prepareForExecutionViaLoginImpl();
			if(prepared && verifyLoggedIn()) {
				final Set<Cookie> cookies = new HashSet<>();
				cookies.addAll(manage().getCookies());
				sessionCookies.put(account, cookies);
				return true;
			}

		} catch(final Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private boolean prepareWithPreExistingSession() {
		try {
			final long start = System.currentTimeMillis();
			System.out.println("Checking if " + this + " already has pre existing cookies...");
			final Set<Cookie> sessionCooks = sessionCookies.computeIfAbsent(account, acc -> new HashSet<>());
			if(cookieCache.computeIfAbsent(this, driver -> new HashSet<>()) != sessionCooks) {

				//web driver spec says we need to land on the page first before setting cookies
				get(getLandingPageURL());
				manage().deleteAllCookies();
				for(final Cookie cookie : sessionCooks) {
					try {
						manage().addCookie(cookie);
					} catch(final UnableToSetCookieException e) {
						e.printStackTrace();
						System.out.println("Unable to set cookie: " + cookie);
					}
				}

				cookieCache.put(this, sessionCooks);
				System.out.println(this + " is done adding pre existing session cookies. Took " + (System.currentTimeMillis() - start) + "ms");
				get(getLandingPageURL());

				if(!verifyLoggedIn()) {
					System.out.println("Failed to verify login for " + this);
					sessionCookies.remove(account);
					return false;
				}

			} else {
				System.out.println(this + " HAS PRE EXISTING COOKIES! Took " + (System.currentTimeMillis() - start) + "ms");
			}

			return true;
		} catch(final Exception e) {
			e.printStackTrace();
			sessionCookies.remove(account);
		}

		return false;
	}
	
	@Override
	public void close() {
		try {
			System.out.println("Closed JBrowserDriver!");
			super.close();
		} catch(final Exception e) {
			//swallow
		}
	}
}
