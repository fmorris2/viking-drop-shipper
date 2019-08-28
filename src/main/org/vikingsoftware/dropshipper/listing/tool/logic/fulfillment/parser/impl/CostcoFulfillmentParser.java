package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.utils.CostcoUtils;
import main.org.vikingsoftware.dropshipper.core.utils.ListingUtils;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.AbstractFulfillmentParser;

public class CostcoFulfillmentParser extends AbstractFulfillmentParser<CostcoWebDriver> {

	@Override
	public Listing parseListing(final String url) {
		try {
			final Listing listing = new Listing();
			listing.fulfillmentPlatformId = FulfillmentPlatforms.COSTCO.getId();
			listing.shippingService = ShippingServiceCodeType.SHIPPING_METHOD_STANDARD;
			
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Parsing listing title"));
			driver.setImplicitWait(2);
			
			final String pageSource = driver.getPageSource();
			listing.title = CostcoUtils.parseListingTitleFromPageSource(pageSource);
			System.out.println("Listing Title: " + listing.title);
			
			System.out.println("Checking 2 day shipping / listing buy limit per membership...");
			if(CostcoUtils.isTwoDayShipping(pageSource) || CostcoUtils.getListingLimitPerMember(pageSource) != -1) {
				System.out.println("\tListing either has 2 day shipping, or a per-member buy limit. Skipping...");
				listing.canShip = false;
				return listing;
			}
			System.out.println("\tdone.");

			System.out.println("Finding add to cart button...");
			try {				
				driver.js("document.getElementById(\"add-to-cart-btn\").getAttribute(\"value\");");
			} catch(final Exception e) {
				System.out.println("\tListing has no add to cart button. Skipping...");
				listing.canShip = false;
				return listing;
			}
			System.out.println("\tdone.");

			listing.canShip = true;

			System.out.println("Parsing item id...");
			final String itemId = CostcoUtils.getProductIdFromPageSource(pageSource);
			listing.itemId = itemId;
			System.out.println("\tListing Item ID: " + listing.itemId);
			
			System.out.println("Parsing listing description...");
			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Parsing listing description"));
			final String featuresHtml = (String)driver.js("document.querySelector(\".features-container > .pdp-features\").getAttribute(\"innerHTML\");");
			listing.description = "<span>Features:</span>\n<ul>\n"+featuresHtml+"</ul>";

			try {
				final String productDetails = driver.findElement(By.cssSelector(".product-info-description")).getAttribute("innerHTML");
				listing.description += "<br /><hr />" + productDetails;
			} catch(final Exception e) {
				//swallow
			}
			System.out.println("\tdone.");

			System.out.println("Making description pretty...");
			ListingUtils.makeDescriptionPretty(listing);
			System.out.println("\tdone.");

			//see if you can get price the same way the inventory updater parses count
			driver.setImplicitWait(1);
			System.out.println("Parsing price...");
			final double price = Double.parseDouble(driver.findElement(By.cssSelector("meta[property=\"product:price:amount\"]")).getAttribute("content"));
			listing.price = price;
			System.out.println("\tdone.");

			try {
				System.out.println("Parsing grocery fee...");
				final String fee = (String)driver.js("document.getElementById(\"grocery-fee-amount\").getAttribute(\"data-grocery-fee-amount\");");
				listing.price += Double.parseDouble(fee.replace("$", ""));
			} catch(final Exception e) {
				System.out.println("\tlisting has no grocery fee");
			}
			System.out.println("\tdone.");
			System.out.println("Listing Price: " + listing.price);

			listing.pictures = getPictures();
			System.out.println("Listing Pictures: " + listing.pictures.size());

			//specifications
			final Map<String, String> specs = getSpecifications();
			listing.brand = specs.getOrDefault("Brand", "Unbranded");
			System.out.println("Listing Brand: " + listing.brand);

			listing.propertyItems = Collections.emptyList();
			listing.variations = Collections.emptyMap();

			return listing;
		} catch(final Exception e) {
			e.printStackTrace();
		} finally {
			driver.resetImplicitWait();
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

	private List<ListingImage> getPictures() throws InterruptedException, MalformedURLException, IOException {
		final List<ListingImage> pics = new ArrayList<>();
		final Supplier<List<WebElement>> thumbnails = () -> driver.findElements(By.cssSelector("#theViews > div > div > a > img"));
		for(final WebElement thumbnail : thumbnails.get()) {
			final String convertedUrl = thumbnail.getAttribute("src").replaceAll("recipeId=\\d+", "recipeId=729");
			pics.add(new ListingImage(convertedUrl));
		}

		if(pics.isEmpty()) {
			final String url = driver.findElement(By.id("initialProductImage")).getAttribute("src");
			pics.add(new ListingImage(url));
		}

		return pics;
	}

	private Map<String, String> getSpecifications() {
		final Map<String, String> specs = new HashMap<>();
		try {
			driver.findElement(By.id("pdp-accordion-header-2")).click();
			final WebElement specificsMaster = driver.findElement(By.id("pdp-accordion-collapse-2"));
			final List<WebElement> rows = specificsMaster.findElements(By.cssSelector(".panel-body .row"));
			for(final WebElement row : rows) {
				final List<WebElement> keyValEls = row.findElements(By.tagName("div"));
				specs.put(keyValEls.get(0).getText(), keyValEls.get(1).getText());
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return specs;
	}

}
