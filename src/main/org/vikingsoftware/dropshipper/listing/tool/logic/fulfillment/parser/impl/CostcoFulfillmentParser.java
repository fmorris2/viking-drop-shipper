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
import java.util.List;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

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

			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Parsing listing title"));
			final Supplier<WebElement> titleEl = () -> driver.findElement(By.cssSelector("#product-details > div.row.visible-xl > div > div.product-h1-container.visible-xl-block > h1"));
			listing.title = driver.waitForTextToAppear(titleEl, 30_000).trim();
			System.out.println("Listing Title: " + listing.title);

			final String itemId = driver.findElement(By.cssSelector(
					"#product-page > div.row.top-content > div.col-xs-12.hidden-xl > div.row.form-group > div > span > span")).getAttribute("textContent");
			listing.itemId = itemId.trim();
			System.out.println("Listing Item ID: " + listing.itemId);

			SwingUtilities.invokeLater(() -> ListingToolGUI.get().statusTextValue.setText("Parsing listing description"));
			final String featuresHtml = driver.findElement(By.cssSelector(".features-container > .pdp-features")).getAttribute("innerHTML");
			listing.description = "<span>Features:</span>\n<ul>\n"+featuresHtml+"</ul>";

			try {
				driver.findElement(By.cssSelector("#view-more > div > input")).click();
				Thread.sleep(1000);
				final String productDetails = driver.findElement(By.cssSelector("#pdp-accordion-collapse-1 > div > div.product-info-description")).getAttribute("innerHTML");
				listing.description += "<br /><hr />" + productDetails;
			} catch(final Exception e) {
				//swallow
			}

			makeDescriptionPretty(listing);

			final double price = Double.parseDouble(driver.findElement(By.cssSelector("#math-table > div.your-price.row.no-gutter > div > span.value")).getText());
			listing.price = price;
			System.out.println("Listing Price: " + listing.price);

			listing.pictures = getPictures();
			System.out.println("Listing Pictures: " + listing.pictures.size());

			listing.propertyItems = Collections.emptyList();
			listing.variations = Collections.emptyMap();

			return listing;
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

	private List<ListingImage> getPictures() throws InterruptedException, MalformedURLException, IOException {
		final List<ListingImage> pics = new ArrayList<>();
		final Supplier<WebElement> thumbnailTrack = () -> driver.findElement(By.cssSelector("#theViews > div > div"));
		final Supplier<List<WebElement>> thumbnails = () -> thumbnailTrack.get().findElements(By.tagName("a"));
		for(int i = 0; i < thumbnails.get().size(); i++) {
			thumbnails.get().get(i).click();
			Thread.sleep(1000);
			final WebElement mainImg = driver.findElement(By.cssSelector("#RICHFXViewerContainer___richfx_id_0_initialImage"));
			final String imgSrc = mainImg.getAttribute("src");
			final BufferedImage img = ImageIO.read(new URL(imgSrc));
			pics.add(new ListingImage(imgSrc, img));
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

		listing.description = out.toString();
	}

}
