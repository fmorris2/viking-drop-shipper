package main.org.vikingsoftware.dropshipper.core.web.aliexpress;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxy;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;


public class AliExpressWebDriver extends LoginWebDriver {
	
	public AliExpressWebDriver() {
		this(null);
	}
	
	public AliExpressWebDriver(final VSDSProxy proxy) {
		super(proxy);
	}

	@Override
	public boolean prepareForExecutionViaLoginImpl() {
		try {
			manage().window().maximize();
			manage().timeouts().implicitlyWait(DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);

			//sign in to ali express
			get("https://login.aliexpress.com/");
			switchTo().frame(findElement(By.id("alibaba-login-box")));

			findElement(By.id("fm-login-id")).sendKeys(account.username);
			findElement(By.id("fm-login-password")).sendKeys(account.password);
			findElement(By.id("fm-login-submit")).click();
			return true;
		} catch(final Exception e) {
			DBLogging.high(getClass(), "failed to prepare for execution: ", e);
		}

		return false;
	}

	@Override
	protected String getLandingPageURL() {
		return "https://www.aliexpress.com";
	}

	@Override
	protected boolean verifyLoggedIn() {
		try {
			//wait for home page to appear
			findElement(By.className("nav-user-account"));
			return true;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void scrollToBottomOfPage() {
		//scroll to bottom of page to load the descrip
		try {
			final Supplier<Integer> pageHeight = () -> Integer.parseInt(((JavascriptExecutor) this).executeScript("return document.body.scrollHeight").toString());
			final Supplier<Integer> currentHeight = () -> Integer.parseInt(((JavascriptExecutor) this).executeScript("return window.pageYOffset").toString());
			while(currentHeight.get() < pageHeight.get() * .85) {
				((JavascriptExecutor) this).executeScript("window.scrollBy(0, 300)", "");
				Thread.sleep(5);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
