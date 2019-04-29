package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.AbstractFulfillmentParser;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.workers.FulfillmentListingParserWorker;

public class CostcoFulfillmentParser extends AbstractFulfillmentParser<CostcoWebDriver> {

	private static CostcoFulfillmentParser instance;

	private CostcoFulfillmentParser() {

	}

	public static synchronized CostcoFulfillmentParser get() {
		if(instance == null) {
			instance = new CostcoFulfillmentParser();
		}

		return instance;
	}

	@Override
	public Listing parseListing() {
		try {
			final Listing listing = new Listing();

			final Supplier<WebElement> titleEl = () -> driver.findElement(By.cssSelector(".product-h1-container > h1"));
			listing.title = driver.waitForTextToAppear(titleEl, 30_000).trim();
			System.out.println("Listing Title: " + listing.title);

			final String featuresHtml = driver.findElement(By.cssSelector(".features-container > .pdp-features")).getAttribute("innerHTML");
			listing.description = "<span>Features:</span><ul class=\"pdp-features\">"+featuresHtml+"</ul>";
			System.out.println("Listing Description: " + listing.description);

			listing.pictures = getPictures();
			listing.propertyItems = Collections.emptyList();
			listing.variations = Collections.emptyMap();

			FulfillmentListingParserWorker.instance().updateStatus("TEST!");
		} catch(final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Class<CostcoDriverSupplier> getDriverSupplierClass() {
		return CostcoDriverSupplier.class;
	}

	@Override
	public boolean needsToLogin() {
		return true;
	}

	private Map<String, BufferedImage> getPictures() {
		final Map<String, BufferedImage> pics = new HashMap<>();

		return pics;
	}


}
