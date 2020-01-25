package main.org.vikingsoftware.dropshipper.core.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.UnableToSetCookieException;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxy;

public abstract class LoginWebDriver extends DefaultWebDriver {

	protected static final int MAX_LOGIN_TRIES = 20;

	private static final Map<FulfillmentAccount, Set<Cookie>> sessionCookies = new ConcurrentHashMap<>();
	private static final Map<LoginWebDriver, Set<Cookie>> cookieCache = new ConcurrentHashMap<>();
	private static final Map<FulfillmentAccount, Object> loginLocks = new ConcurrentHashMap<>();

	protected final Map<String,String> cachedOrderOptions = new HashMap<>();

	protected int loginTries = 0;
	protected FulfillmentAccount account;

	protected abstract boolean prepareForExecutionViaLoginImpl();
	protected abstract String getLandingPageURL();
	protected abstract boolean verifyLoggedIn();
	
	public LoginWebDriver() {
		this(null);
	}
	
	public LoginWebDriver(final VSDSProxy proxy) {
		super(proxy);
	}
	
	public static void clearSessionCaches() {
		sessionCookies.clear();
		cookieCache.clear();
		loginLocks.clear();
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

	public void clearSession() {
		if(account != null) {
			sessionCookies.put(account, new HashSet<>());
		}
	}

	public void clearCachedSelectedOrderOptions() {
		cachedOrderOptions.clear();
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
}
