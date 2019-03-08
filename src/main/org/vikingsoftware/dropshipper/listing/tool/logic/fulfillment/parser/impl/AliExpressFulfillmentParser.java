package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.AliExpressWebDriverQueue;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.PropertyItem;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.PropertyItemOption;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.FulfillmentParser;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

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
	
	private Listing parseListing(final AliExpressWebDriver driver) throws InterruptedException, MalformedURLException, IOException {
		final Listing listing = new Listing();
		listing.title = driver.findElement(By.className("product-name")).getText().trim();		
		listing.pictures = getPictures(driver);
		listing.propertyItems = getPropertyItems(driver);
		listing.description = getDescription(driver);
		
		System.out.println("listing title: " + listing.title);
		System.out.println("listing pics: " + listing.pictures.keySet());
		System.out.println("listing description: " + listing.description);
		
		return listing;
	}
	
	private Map<String, BufferedImage> getPictures(final AliExpressWebDriver driver) throws InterruptedException, MalformedURLException, IOException {
		final Map<String, BufferedImage> pics = new HashMap<>();
		
		final WebElement thumbsEl = driver.findElement(By.id("j-image-thumb-list"));
		final int numThumbNails = thumbsEl.findElements(By.className("img-thumb-item")).size();
		
		final Supplier<String> fullImg = () -> {
			final WebElement fullPicFrame = driver.findElement(By.className("ui-image-viewer-thumb-frame"));
			final WebElement img = fullPicFrame.findElement(By.tagName("img"));
			return img.getAttribute("src");
		};
		
		String currentImg = null;
		for(int i = 0; i < numThumbNails; i++) {
			try {
				final WebElement thumbList = driver.findElement(By.id("j-image-thumb-list"));
				final List<WebElement> thumbNails = thumbList.findElements(By.className("img-thumb-item"));
				thumbNails.get(i).findElement(By.tagName("img")).click();
				while(currentImg != null && currentImg.equals(fullImg.get())) {
					Thread.sleep(10);
				}
				currentImg = fullImg.get();
				pics.put(currentImg, ImageIO.read(new URL(currentImg)));
			} catch(final StaleElementReferenceException e) {
				i--;
			}
			Thread.sleep(50);
		}
		
		return pics;
	}
	
	private List<PropertyItem> getPropertyItems(final AliExpressWebDriver driver) {
		final List<PropertyItem> items = new ArrayList<>();
		final WebElement productInfoEl = driver.findElement(By.id("j-product-info-sku"));
		final List<WebElement> propertyItemsEls = productInfoEl.findElements(By.className("p-property-item"));
		for(final WebElement propertyItemEl : propertyItemsEls) {
			final PropertyItem item = new PropertyItem();
			item.name = propertyItemEl.findElement(By.className("p-item-title")).getText().replace(":", "");
			final WebElement skuList = propertyItemEl.findElement(By.className("sku-attr-list"));
			final List<WebElement> skus = skuList.findElements(By.tagName("li"));
			for(final WebElement sku : skus) {
				if(sku.getAttribute("class").contains("disabled")) {
					System.out.println("Skipping disabled sku...");
					continue;
				}
				final PropertyItemOption option = new PropertyItemOption();
				final WebElement skuInfoEl = sku.findElement(By.tagName("a"));
				option.elementId = skuInfoEl.getAttribute("id");
				option.skuId = skuInfoEl.getAttribute("data-sku-id");
				option.title = skuInfoEl.getAttribute("title");
				
				try {
					final WebElement thumbnailEl = skuInfoEl.findElement(By.tagName("img"));
					option.thumbNailUrl = thumbnailEl.getAttribute("src");
				} catch(final Exception e) {
					System.out.println("Encountered non-image sku");
				}
				
				item.options.add(option);
				sku.click();
			}
			
			items.add(item);
		}
		return items;
	}
	
	private String getDescription(final AliExpressWebDriver driver) throws InterruptedException {
		driver.scrollToBottomOfPage();
		while(driver.findElement(By.className("description-content")).findElements(By.xpath(".//*")).size() <= 1) {
			Thread.sleep(10);
		}
		return driver.findElement(By.className("description-content")).getAttribute("outerHTML");
	}
	
	public static AliExpressFulfillmentParser get() {
		if(instance == null) {
			instance = new AliExpressFulfillmentParser();
		}
		
		return instance;
	}

}
