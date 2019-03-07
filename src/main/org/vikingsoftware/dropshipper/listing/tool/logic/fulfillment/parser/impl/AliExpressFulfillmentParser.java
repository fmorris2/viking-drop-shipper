package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.util.function.Supplier;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriverQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParser;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class AliExpressFulfillmentParser extends AliExpressWebDriverQueue implements FulfillmentParser {
	
	private static AliExpressFulfillmentParser instance;
	
	private AliExpressFulfillmentParser() { 
		super();
	}
	
	@Override
	public Listing getListingTemplate(final String url) {
		
		AliExpressDriverSupplier supplier = null;
		AliExpressWebDriver driver = null;
		
		try {
			supplier = webDrivers.take();
			driver = supplier.get();
			
			if(driver.getReady()) {
				driver.get(url);
				return parseListing(driver);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			if(supplier != null) {
				webDrivers.add(supplier);
			}
		}
		return null;
	}
	
	private Listing parseListing(final WebDriver driver) throws InterruptedException {
		//scroll to bottom of page to load the descrip
		final Supplier<Integer> pageHeight = () -> Integer.parseInt(((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight").toString());
		final Supplier<Integer> currentHeight = () -> Integer.parseInt(((JavascriptExecutor) driver).executeScript("return window.pageYOffset").toString());
		while(currentHeight.get() < pageHeight.get() * .85) {
			System.out.println("curr: " + currentHeight.get() + ", page: " + pageHeight.get());
			((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300)", "");
			Thread.sleep(5);
		}
		while(driver.findElement(By.className("description-content")).findElements(By.xpath(".//*")).size() <= 1) {
			Thread.sleep(10);
		}
		final Listing listing = new Listing();
		listing.title = driver.findElement(By.className("product-name")).getText().trim();
		listing.description = driver.findElement(By.className("description-content")).getAttribute("outerHTML");
		
		System.out.println("listing title: " + listing.title);
		System.out.println("listing description: " + listing.description);
		
		return listing;
	}
	
	public static AliExpressFulfillmentParser get() {
		if(instance == null) {
			instance = new AliExpressFulfillmentParser();
		}
		
		return instance;
	}

}
