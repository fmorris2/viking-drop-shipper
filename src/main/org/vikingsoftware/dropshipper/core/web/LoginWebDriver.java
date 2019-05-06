package main.org.vikingsoftware.dropshipper.core.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.UnableToSetCookieException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.listing.FulfillmentListing;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMapping;

public abstract class LoginWebDriver extends ChromeDriver {

	public static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 10;

	protected static final int MAX_LOGIN_TRIES = 20;

	private static final boolean HEADLESS = false;
	private static final ChromeOptions OPTIONS = generateOptions();

	private static final Map<FulfillmentAccount, Set<Cookie>> sessionCookies = new ConcurrentHashMap<>();
	private static final Map<LoginWebDriver, Set<Cookie>> cookieCache = new ConcurrentHashMap<>();
	private static final Map<FulfillmentAccount, Object> loginLocks = new ConcurrentHashMap<>();

	protected final Map<String,String> cachedOrderOptions = new HashMap<>();

	protected int loginTries = 0;
	protected FulfillmentAccount account;

	public LoginWebDriver() {
		super(OPTIONS);
	}

	public abstract boolean selectOrderOptions(final SkuMapping skuMapping, final FulfillmentListing listing);

	protected abstract boolean prepareForExecutionViaLoginImpl();
	protected abstract String getLandingPageURL();
	protected abstract boolean verifyLoggedIn();

	public boolean getReady(final FulfillmentAccount account) {
		this.account = account;
		try {
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
		} catch(final NoSuchSessionException e) {
			e.printStackTrace();
		}

		return false;
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
			final String txt = element.get().getText();
			if(txt != null && !txt.isEmpty()) {
				return txt;
			}
		}

		return null;
	}

	public void sendKeysSlowly(final WebElement el, final String keys) throws InterruptedException {
		final char[] chars = keys.toCharArray();
		for(final char character : chars) {
			el.sendKeys(""+character);
			Thread.sleep(150);
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
				System.out.println(this + " is done adding pre existing session cookies");
				get(getLandingPageURL());

				if(!verifyLoggedIn()) {
					System.out.println("Failed to verify login for " + this);
					sessionCookies.remove(account);
					return false;
				}

			} else {
				System.out.println(this + " HAS PRE EXISTING COOKIES!");
			}

			return true;
		} catch(final Exception e) {
			e.printStackTrace();
			sessionCookies.remove(account);
		}

		return false;
	}

	protected static ChromeOptions generateOptions() {
		final ChromeOptions options = new ChromeOptions();
		options.setHeadless(HEADLESS);
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disk-cache-size=4096");
		options.addArguments("--profile.managed_default_content_settings.images=2", "--blink-settings=imagesEnabled=false");
		return options;
	}
}
