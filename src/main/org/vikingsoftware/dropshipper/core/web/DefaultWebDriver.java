package main.org.vikingsoftware.dropshipper.core.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
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
	
	@Override
	public String getPageSource() {
		final long start = System.currentTimeMillis();
		String source = null;
		while(source == null && System.currentTimeMillis() - start < 10_000) {
			try {
				source = super.getPageSource();
			} catch(final Exception e) {
				//swallow exception
			}
			
			sleep(10);
		}
		
		if(source == null) {
			throw new RuntimeException("Failed to get page source");
		}
		
		return source;
	}
	
	public void savePageSource() {
		savePageSource("current-page-source");
	}
	
	public void savePageSource(final String str) {
		try(final FileWriter fW = new FileWriter(str);
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
	
	public void screenshot(final String filePath) {
		final OutputType<File> output = new OutputType<File>() {
		    @Override
		    public File convertFromBase64Png(String base64Png) {
		      return save(BYTES.convertFromBase64Png(base64Png));
		    }

		    @Override
		    public File convertFromPngBytes(byte[] data) {
		      return save(data);
		    }

		    private File save(byte[] data) {
		      OutputStream stream = null;

		      try {
		        File tmpFile = new File(filePath);

		        stream = new FileOutputStream(tmpFile);
		        stream.write(data);

		        return tmpFile;
		      } catch (IOException e) {
		        throw new WebDriverException(e);
		      } finally {
		        if (stream != null) {
		          try {
		            stream.close();
		          } catch (IOException e) {
		            // Nothing sane to do
		          }
		        }
		      }
		    }

		    public String toString() {
		      return "OutputType.FILE";
		    }
		  };
		  
		  getScreenshotAs(output);
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
