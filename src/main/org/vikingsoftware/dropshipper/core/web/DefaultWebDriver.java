package main.org.vikingsoftware.dropshipper.core.web;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;

public class DefaultWebDriver extends JBrowserDriver {
	
	private static final int DEFAULT_TIMEOUT_MS = 30_000;
	public static final int DEFAULT_VISIBILITY_WAIT_SECONDS = 20;
	
	public DefaultWebDriver() {
		super(getSettingsBuilder().build());
	}
	
	public DefaultWebDriver(final Settings settings) {
		super(settings);
	}
	
	public static Settings.Builder getSettingsBuilder() {
		return new Settings.Builder()
			.headless(true)
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
	
	public WebElement findElementNormal(final By by) {
		return super.findElement(by);
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
