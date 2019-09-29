package main.org.vikingsoftware.dropshipper.crawler.strategy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import main.org.vikingsoftware.dropshipper.core.web.DefaultWebDriver;
import main.org.vikingsoftware.dropshipper.crawler.strategy.FulfillmentListingCrawlerStrategy;

public class SamsClubCrawlerStrategy extends FulfillmentListingCrawlerStrategy {

	private static final String ALL_CATEGORIES_URL = "https://www.samsclub.com/c";
	
	private static final String CATEGORY_PAGE_SUBSTRING = "/c/";
	private static final String SUB_CATEGORY_PAGE_SUBSTRING = "/b/";
	private static final String PRODUCT_PAGE_SUBSTRING = "/p/";
	
	private static final String ONLINE_ONLY_FILTER = "?selectedFilter=online";
	
	private final Set<String> visitedUrls = new HashSet<>();
	
	private DefaultWebDriver driver = null;
	
	public void crawl() {
		try {
			driver = new DefaultWebDriver();
//			driver = new DefaultWebDriver(DefaultWebDriver.getSettingsBuilder()
//					.blockMedia(true)
//					.blockAds(true)
//					.quickRender(true)
//					.build());
			System.out.println("[SamsClubCrawlerStrategy] - crawl");
			crawlCategoryPage(ALL_CATEGORIES_URL);
		} finally {
			driver.close();
		}
	}
	
	private void crawlCategoryPage(final String url) {
		System.out.println("[SamsClubCrawlerStrategy] - crawling category page: " + url);
		visitedUrls.add(url);
		System.out.println("[SamsClubCrawlerStrategy] - loading page...");
		driver.get(url);
		System.out.println("\tdone.");
		
		final List<String> linksOnPage = new ArrayList<>(parseAllLinksOnPage());
		Collections.shuffle(linksOnPage);
		
		for(final String link : linksOnPage) {
			final PageType pageType = getPageTypeForUrl(link);
			System.out.println(pageType + ": " + link);
			switch(pageType) {
				case CATEGORY:
					System.out.println("[SamsClubCrawlerStrategy] - Found category link on page: " + link);
					crawlCategoryPage(link);
				break;
				case SUB_CATEGORY:
					System.out.println("[SamsClubCrawlerStrategy] - Found sub category link on page: " + link);
					crawlSubCategoryPage(link + ONLINE_ONLY_FILTER);
				break;
				default:
			}
		}
	}
	
	private Set<String> parseAllLinksOnPage() {
		final Set<String> links = new HashSet<>();
		try {
			System.out.println("parseAllLinksOnPage");
			final Supplier<List<WebElement>> aElements = () -> driver.findElements(By.tagName("a"));
			for(final WebElement el : aElements.get()) {
				try {
					final String href = el.getAttribute("href");
					if(href != null && href.contains("samsclub.com/") && !visitedUrls.contains(href)) {
						final int queryStart = href.indexOf("?");
						if(queryStart != -1) {
							links.add(href.substring(0, queryStart));
						} else {
							links.add(href);
						}
					}
				} catch(final StaleElementReferenceException e) {
					return parseAllLinksOnPage();
				}
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return links;
	}
	
	private void crawlSubCategoryPage(final String url) {
		System.out.println("[SamsClubCrawlerStrategy] - Attempting to crawl sub category page");
		visitedUrls.add(url);
		driver.get(url);
		try {
			int numProducts = parseProductsOnCurrentPage();
			
			while(numProducts > 0) {
				driver.setImplicitWait(1);
				try {
					System.out.println("[SamsClubCrawlerStrategy] - Navigating to next page of products...");
					driver.js("document.querySelector(\".sc-pagination-next > a:nth-child(1)\").click();");
					driver.sleep(3000); //sluggish sams club front end
					numProducts = parseProductsOnCurrentPage();
				} catch(final JavascriptException e) {
					numProducts = 0;
				} catch(final Exception e) {
					e.printStackTrace();
					numProducts = 0;
				}
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private int parseProductsOnCurrentPage() {
		System.out.println("[SamsClubCrawlerStrategy] - Parsing products on current page");
		int numProducts = 0;
		final Set<String> links = parseAllLinksOnPage();
		visitedUrls.addAll(links);
		
		for(final String url : links) {
			if(getPageTypeForUrl(url) == PageType.PRODUCT) {
				System.out.println("[SamsClubCrawlerStrategy] - Found product url: " + url);
				urlFound(url);
				numProducts++;
			}
		}	
		
		return numProducts;
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
