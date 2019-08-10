package main.org.vikingsoftware.dropshipper.crawler.strategy.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;
import main.org.vikingsoftware.dropshipper.crawler.strategy.FulfillmentListingCrawlerStrategy;

public class SamsClubCrawlerStrategy extends FulfillmentListingCrawlerStrategy {

	private static final String ALL_CATEGORIES_URL = "https://www.samsclub.com/c?xid=hdr_shop1_all-departments";
	
	private static final String CATEGORY_PAGE_SUBSTRING = "/c/";
	private static final String SUB_CATEGORY_PAGE_SUBSTRING = "/b/";
	private static final String PRODUCT_PAGE_SUBSTRING = "/p/";
	
	private final SamsClubDriverSupplier supplier = BrowserRepository.get().request(SamsClubDriverSupplier.class);
	private final SamsClubWebDriver driver = supplier.get();
	private final Set<String> visitedUrls = new HashSet<>();
	
	public void crawl() {
		System.out.println("[SamsClubCrawlerStrategy] - crawl");

		crawlCategoryPage(ALL_CATEGORIES_URL);
		visitedUrls.clear();
	}
	
	private void crawlCategoryPage(final String url) {
		System.out.println("[SamsClubCrawlerStrategy] - crawling category page: " + url);
		visitedUrls.add(url);
		driver.get(url);
		
		final Set<String> linksOnPage = parseAllLinksOnPage();
		
		visitedUrls.addAll(linksOnPage);
		
		for(final String link : linksOnPage) {
			final PageType pageType = getPageTypeForUrl(link);
			switch(pageType) {
				case CATEGORY:
					System.out.println("[SamsClubCrawlerStrategy] - Found category link on page: " + link);
					crawlCategoryPage(link);
				break;
				case SUB_CATEGORY:
					System.out.println("[SamsClubCrawlerStrategy] - Found sub category link on page: " + link);
					crawlSubCategoryPage(link);
				break;
				case PRODUCT:
					System.out.println("[SamsClubCrawlerStrategy] - Found product link on page: " + link);
					urlFound(link);
				break;
				default:
			}
		}
	}
	
	private Set<String> parseAllLinksOnPage() {
		try {
			return driver.findElements(By.tagName("a")).stream()
				.map(element -> {
					final String href = element.getAttribute("href");
					if(href != null && !href.isEmpty() && !href.startsWith("http")) {
						return "https://www.samsclub.com" + href;
					}
					
					return href;
				})
				.filter(str -> str != null && !str.isEmpty() && !visitedUrls.contains(str))
				.collect(Collectors.toSet());
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return Collections.emptySet();
	}
	
	private void crawlSubCategoryPage(final String url) {
		driver.get(url);
		try {
			driver.findElement(By.id("filter-online-id")).click();
			driver.sleep(2000); //wait for sluggish sams club front-end to update
			
			parseProductsOnCurrentPage();
			
			driver.setImplicitWait(1);
			try {
				final WebElement nextButton = driver.findElement(By.cssSelector(".sc-pagination-next > a:nth-child(1)"));
				System.out.println("[SamsClubCrawlerStrategy] - Navigating to next page of products...");
				nextButton.click();
				driver.sleep(2000); //sluggish sams club front end
				parseProductsOnCurrentPage();
			} catch(final Exception e) {}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parseProductsOnCurrentPage() {
		final Set<String> links = parseAllLinksOnPage();
		visitedUrls.addAll(links);
		
		for(final String url : visitedUrls) {
			if(getPageTypeForUrl(url) == PageType.PRODUCT) {
				System.out.println("[SamsClubCrawlerStrategy] - Found product url: " + url);
				urlFound(url);
			}
		}
		
	}
	
	private PageType getPageTypeForUrl(final String url) {
		if(url.contains(CATEGORY_PAGE_SUBSTRING)) {
			return PageType.CATEGORY;
		} 
		
		if(url.contains(SUB_CATEGORY_PAGE_SUBSTRING)) {
			return PageType.SUB_CATEGORY;
		}
		
		if(url.contains(PRODUCT_PAGE_SUBSTRING)) {
			return PageType.PRODUCT;
		}
		
		return PageType.UNKNOWN;
	}
	
	private enum PageType {
		CATEGORY,
		SUB_CATEGORY,
		PRODUCT,
		UNKNOWN
	}

}
