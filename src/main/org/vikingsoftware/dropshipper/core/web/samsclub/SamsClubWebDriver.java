package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class SamsClubWebDriver extends LoginWebDriver {
	
	private static final int MAX_LOGIN_ATTEMPTS = 2;
	private static final long VERIFY_LOGGED_IN_WAIT = 4000;
	
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	private static final Queue<LoginFormElementFindingStrategy> usernameElStrats = new PriorityQueue<>(Arrays.asList(
		new LoginFormElementFindingStrategy(driver -> driver.findElementNormal(By.id("txtLoginEmailID"))),
		new LoginFormElementFindingStrategy(driver -> driver.findElementNormal(By.id("email")))
	));
	
	private static final Queue<LoginFormElementFindingStrategy> passwordElStrats = new PriorityQueue<>(Arrays.asList(
		new LoginFormElementFindingStrategy(driver -> driver.findElementNormal(By.id("txtLoginPwd"))),
		new LoginFormElementFindingStrategy(driver -> driver.findElementNormal(By.id("password")))
	));
	
	private static final Queue<LoginFormElementFindingStrategy> buttonElStrats = new PriorityQueue<>(Arrays.asList(
		new LoginFormElementFindingStrategy(driver -> driver.findElementNormal(By.cssSelector("#signInButton"))),
		new LoginFormElementFindingStrategy(driver -> driver.findElementNormal(By.className("sc-btn-primary")))
	));
	
	private int loginAttempts = 0;
	
	private WebElement usernameEl, passwordEl, buttonEl;
	
	public SamsClubWebDriver() {
		this(null);
	}
	
	public SamsClubWebDriver(final VSDSProxy proxy) {
		super(proxy);
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
			savePageSource("sams-login-page.html");
			
			this.setImplicitWait(1);
			usernameEl = findElementViaOptimalStrategy(usernameElStrats);
			passwordEl = findElementViaOptimalStrategy(passwordElStrats);
			buttonEl = findElementViaOptimalStrategy(buttonElStrats);
			this.resetImplicitWait();
			
			if(usernameEl != null && passwordEl != null && buttonEl != null) {
				System.out.println("Logging in... " + getCurrentUrl());
				usernameEl.sendKeys(account.username);
				passwordEl.sendKeys(account.password);
				buttonEl.click();
				final long start = System.currentTimeMillis();
				boolean loggedIn = false;
				while(!(loggedIn = verifyLoggedIn()) && System.currentTimeMillis() - start < VERIFY_LOGGED_IN_WAIT) {
					sleep(10);
				}
				return loggedIn;
			}
			
			return false;
		} catch(final Exception e) {
			e.printStackTrace();
			return prepareForExecutionViaLoginImpl();
		} finally {
			loginAttempts = 0;
		}
	}
	
	private WebElement findElementViaOptimalStrategy(final Queue<LoginFormElementFindingStrategy> strats) {
		WebElement toReturn = null;
		lock.writeLock().lock();
		final List<LoginFormElementFindingStrategy> attemptedStrats = new ArrayList<>();
		try {
			while(!strats.isEmpty()) {
				final LoginFormElementFindingStrategy strat = strats.poll();
				attemptedStrats.add(strat);
				toReturn = strat.apply(this);
				if(toReturn != null) {
					break;
				}
			}
		} finally {
			strats.addAll(attemptedStrats);
			lock.writeLock().unlock();
		}
		return toReturn;
	}

	@Override
	protected String getLandingPageURL() {
		return "https://www.samsclub.com";
	}

	@Override
	protected boolean verifyLoggedIn() {
		final Cookie cookie = this.manage().getCookieNamed("signedIn");
		return cookie != null && cookie.getValue().equalsIgnoreCase("Y");
	}
	
	private static class LoginFormElementFindingStrategy implements Function<LoginWebDriver, WebElement>, Comparable<LoginFormElementFindingStrategy> {
		
		private final Function<LoginWebDriver, WebElement> wrappedFunction;
		
		private int successes = 0;
		
		public LoginFormElementFindingStrategy(final Function<LoginWebDriver, WebElement> strategy) {
			this.wrappedFunction = strategy;
		}
		
		@Override
		public WebElement apply(final LoginWebDriver driver) {
			WebElement toReturn = null;
			try {
				toReturn = wrappedFunction.apply(driver);
				successes++;
			} catch(final Exception e) {
				//swallow
			}
			
			return toReturn;
		}

		@Override
		public int compareTo(LoginFormElementFindingStrategy o) {
			return o.successes - this.successes;
		}
		
	}
}
