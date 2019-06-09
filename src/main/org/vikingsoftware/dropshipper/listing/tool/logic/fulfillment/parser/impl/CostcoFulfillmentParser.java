package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import com.ebay.soap.eBLBaseComponents.ShippingServiceCodeType;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentPlatforms;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.costco.CostcoWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.gui.ListingToolGUI;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.ListingImage;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.AbstractFulfillmentParser;

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
			listing.fulfillmentPlatformId = FulfillmentPlatforms.COSTCO.getId();
			listing.shippingService = ShippingServiceCodeType.SHIPPING_METHOD_STANDARD;

			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Parsing listing title"));
			final Supplier<WebElement> titleEl = () -> driver.findElement(By.cssSelector("#product-details h1"));
			listing.title = driver.waitForTextToAppear(titleEl, 30_000).trim();
			System.out.println("Listing Title: " + listing.title);
			
			driver.setImplicitWait(1);
			try {
				final WebElement twoDayImage = driver.findElement(By.cssSelector(".col-xs-5 > a:nth-child(1) > img:nth-child(1)"));
				if(twoDayImage.getAttribute("alt").contains("2-day")) {
					System.out.println("listing is 2-day!");
					listing.canShip = false;
					return listing;
				}
			} catch(final Exception e) {
				//swallow
			}

			try {
				driver.findElement(By.id("add-to-cart-btn"));
			} catch(final Exception e) {
				listing.canShip = false;
				return listing;
			}

			listing.canShip = true;

			final String itemId = driver.findElement(By.cssSelector(
					"#product-page span[itemprop=\"sku\"]")).getAttribute("textContent");
			listing.itemId = itemId.trim();
			System.out.println("Listing Item ID: " + listing.itemId);

			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Parsing listing description"));
			final String featuresHtml = driver.findElement(By.cssSelector(".features-container > .pdp-features")).getAttribute("innerHTML");
			listing.description = "<span>Features:</span>\n<ul>\n"+featuresHtml+"</ul>";

			try {
				final String productDetails = driver.findElement(By.cssSelector(".product-info-description")).getAttribute("innerHTML");
				listing.description += "<br /><hr />" + productDetails;
			} catch(final Exception e) {
				//swallow
			}

			System.out.println("Making description pretty...");
			makeDescriptionPretty(listing);
			System.out.println("\tdone.");

			//see if you can get price the same way the inventory updater parses count
			driver.setImplicitWait(1);
			final double price = Double.parseDouble(driver.findElement(By.cssSelector("meta[property=\"product:price:amount\"]")).getAttribute("content"));
			listing.price = price;

			try {
				driver.setImplicitWait(2);
				final String fee = driver.findElement(By.id("grocery-fee-amount")).getAttribute("data-grocery-fee-amount").replace("$", "");
				listing.price += Double.parseDouble(fee);
			} catch(final Exception e) {
				e.printStackTrace();
			} finally {
				driver.resetImplicitWait();
			}
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
			final String convertedUrl = thumbnail.getAttribute("src").replace("recipeId=735", "recipeId=729");
			final BufferedImage img = ImageIO.read(new URL(convertedUrl));
			pics.add(new ListingImage(convertedUrl, img));
		}

		if(pics.isEmpty()) {
			final String url = driver.findElement(By.id("initialProductImage")).getAttribute("src");
			final BufferedImage img = ImageIO.read(new URL(url));
			pics.add(new ListingImage(url, img));
		}

		return pics;
	}

	private void makeDescriptionPretty(final Listing listing) {
		final Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		tidy.setIndentContent(true);
		tidy.setPrintBodyOnly(true);
		tidy.setTidyMark(false);

		final Document htmlDom = tidy.parseDOM(new ByteArrayInputStream(listing.description.getBytes()), null);
		final OutputStream out = new ByteArrayOutputStream();
		tidy.pprint(htmlDom, out);

		String replacedDesc = out.toString();
		replacedDesc = replacedDesc.replaceAll("<!DOCTYPE[^>]*(>?)", "");
		replacedDesc = replacedDesc.replaceAll("<\\/*html.*>", "");
		replacedDesc = replacedDesc.replaceAll("<head>(?:.|\\n|\\r)+?<\\/head>", "");
		replacedDesc = replacedDesc.replaceAll("<\\/*body>", "");
		replacedDesc = replacedDesc.replaceAll("(\\\"\\s*|;\\s*)(max-)*width\\:\\s*([1-9]\\d{3,}|9[3-9]\\d|9[4-9]{2})(px|%)(\\s*!important)*", "\1\2width:923px!important");
		replacedDesc = replacedDesc.replaceAll("width=\"([1-9]\\d{3,}|9[3-9]\\d|9[4-9]{2})\"", "width=\"923\"");
		replacedDesc = replacedDesc.replaceAll("height=\"\\d+\"", "");

		listing.description =
				"<div style=\"font-family: Helvetica; max-width: 923px\">" +
						replacedDesc +
				"</div>";
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
