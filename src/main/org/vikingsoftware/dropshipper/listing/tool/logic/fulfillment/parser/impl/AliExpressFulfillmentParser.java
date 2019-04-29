package main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.core.web.aliexpress.AliExpressWebDriver;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.PropertyItem;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.PropertyItemOption;
import main.org.vikingsoftware.dropshipper.listing.tool.logic.fulfillment.parser.AbstractFulfillmentParser;

public class AliExpressFulfillmentParser extends AbstractFulfillmentParser<AliExpressWebDriver> {

	private static final int IMG_CHANGE_TIMEOUT_MS = 2000;

	private static AliExpressFulfillmentParser instance;


	private final Set<PropertyItemOption> currentlySelectedOptions = new HashSet<>();
	private AliExpressFulfillmentParser() {
		super();
	}

	@Override
	protected Listing parseListing() {
		try {
			final Listing listing = new Listing();
			listing.title = driver.findElement(By.className("product-name")).getText().trim();
			listing.pictures = getPictures(driver);
			listing.propertyItems = getPropertyItems(driver);
			listing.variations = getVariations(driver, listing);
			listing.description = getDescription(driver);

			System.out.println("listing title: " + listing.title);
			System.out.println("listing pics: " + listing.pictures.keySet());
			System.out.println("listing description: " + listing.description);
			System.out.println("listing variations: " + listing.variations);
			return listing;
		} catch(final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Map<String, BufferedImage> getPictures(final AliExpressWebDriver driver) throws InterruptedException, MalformedURLException, IOException {
		final Map<String, BufferedImage> pics = new HashMap<>();

		final WebElement thumbsEl = driver.findElement(By.id("j-image-thumb-list"));
		final int numThumbNails = thumbsEl.findElements(By.className("img-thumb-item")).size();
		final Supplier<String> fullImg = getMainImg(driver);

		String currentImg = null;

		for(int i = 0; i < numThumbNails; i++) {
			try {
				final WebElement thumbList = driver.findElement(By.id("j-image-thumb-list"));
				final List<WebElement> thumbNails = thumbList.findElements(By.className("img-thumb-item"));
				thumbNails.get(i).findElement(By.tagName("img")).click();
				final String curr = currentImg;
				waitForImgChange(() -> curr != null && curr.equals(fullImg.get()));
				currentImg = fullImg.get();
				pics.put(currentImg, ImageIO.read(new URL(currentImg)));
			} catch(final StaleElementReferenceException e) {
				i--;
			}
			Thread.sleep(50);
		}

		return pics;
	}

	private Supplier<String> getMainImg(final WebDriver driver) {
		return () -> {
			final WebElement fullPicFrame = driver.findElement(By.className("ui-image-viewer-thumb-frame"));
			final WebElement img = fullPicFrame.findElement(By.tagName("img"));
			return img.getAttribute("src");
		};
	}

	private List<PropertyItem> getPropertyItems(final AliExpressWebDriver driver) throws InterruptedException {
		final List<PropertyItem> items = new ArrayList<>();
		final WebElement productInfoEl = driver.findElement(By.id("j-product-info-sku"));
		final List<WebElement> propertyItemsEls = productInfoEl.findElements(By.className("p-property-item"));

		for(final WebElement propertyItemEl : propertyItemsEls) {
			final PropertyItem item = new PropertyItem();
			item.name = propertyItemEl.findElement(By.className("p-item-title")).getText().replace(":", "");
			final WebElement skuList = propertyItemEl.findElement(By.className("sku-attr-list"));
			final List<WebElement> skus = skuList.findElements(By.tagName("li"));
			final Supplier<String> mainImg = getMainImg(driver);
			String prevMainImg = mainImg.get();

			for(final WebElement sku : skus) {
				if(sku.getAttribute("class").contains("disabled")) {
					System.out.println("Skipping disabled sku...");
					continue;
				}
				final PropertyItemOption option = new PropertyItemOption();
				final WebElement skuInfoEl = sku.findElement(By.tagName("a"));
				option.elementId = skuInfoEl.getAttribute("id");
				option.skuId = skuInfoEl.getAttribute("data-sku-id");

				try {
					driver.manage().timeouts().implicitlyWait(10, TimeUnit.MILLISECONDS);
					final WebElement thumbnailEl = skuInfoEl.findElement(By.tagName("img"));
					option.thumbNailUrl = thumbnailEl.getAttribute("src");
					option.title = skuInfoEl.getAttribute("title");
					driver.manage().timeouts().implicitlyWait(LoginWebDriver.DEFAULT_VISIBILITY_WAIT_SECONDS, TimeUnit.SECONDS);
				} catch(final Exception e) {
					System.out.println("Encountered non-image sku");
					option.title = skuInfoEl.findElement(By.tagName("span")).getText();
				}

				sku.click();

				final String prev = prevMainImg;
				waitForImgChange(() -> mainImg.get().equals(prev));

				prevMainImg = mainImg.get();
				option.fullImageUrl = prevMainImg;

				System.out.println("Adding item option: " + option);
				item.options.add(option);
			}

			items.add(item);
		}
		return items;
	}

	private Map<Set<PropertyItemOption>, Double> getVariations(final WebDriver driver, final Listing listing) {
		final Map<Set<PropertyItemOption>, Double> variations = new HashMap<>();
		if(!listing.propertyItems.isEmpty()) {
			final PropertyItem first = listing.propertyItems.get(0);
			selectVariationRecursively(driver, variations, first, listing.propertyItems.subList(1, listing.propertyItems.size()));
		}

		currentlySelectedOptions.clear();
		System.out.println("variations: " + variations);
		System.out.println("# of variations: " + variations.size());
		return variations;
	}

	private void selectVariationRecursively(final WebDriver driver, final Map<Set<PropertyItemOption>, Double> variations,
			final PropertyItem current, final List<PropertyItem> remaining) {

		for(final PropertyItemOption opt : current.options) {
			currentlySelectedOptions.add(opt);
			driver.findElement(By.id(opt.elementId)).click();
			if(!remaining.isEmpty()) {
				selectVariationRecursively(driver, variations, remaining.get(0), remaining.subList(1, remaining.size()));
			}

			final double price = Double.parseDouble(driver.findElement(By.id("j-sku-price")).getText());
			if(!currentlySelectedOptions.isEmpty()) {
				variations.put(new HashSet<>(currentlySelectedOptions), price);
				currentlySelectedOptions.remove(opt);
			}
		}

		if(remaining.isEmpty()) {
			currentlySelectedOptions.clear();
		}
	}

	private void waitForImgChange(final Supplier<Boolean> waitCondition) throws InterruptedException {
		final long start = System.currentTimeMillis();
		while(waitCondition.get() && System.currentTimeMillis() - start < IMG_CHANGE_TIMEOUT_MS) {
			Thread.sleep(50);
		}
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

	@Override
	public Class<AliExpressDriverSupplier> getDriverSupplierClass() {
		return AliExpressDriverSupplier.class;
	}

	@Override
	public boolean needsToLogin() {
		return false;
	}

}
