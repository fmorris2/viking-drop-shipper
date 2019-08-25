package main.org.vikingsoftware.dropshipper.crawler.strategy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.By;

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
			driver = new DefaultWebDriver(DefaultWebDriver.getSettingsBuilder()
					.blockMedia(true)
					.blockAds(true)
					.quickRender(true)
					.build());
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
		try {
			return driver.findElements(By.tagName("a")).stream()
				.map(element -> {
					final String href = element.getAttribute("href");
					if(href != null && !href.isEmpty() && !href.startsWith("http")) {
						return trimUrl("https://www.samsclub.com" + href);
					}
					
					return trimUrl(href);
				})
				.filter(str -> str != null && !str.isEmpty() && !visitedUrls.contains(str))
				.collect(Collectors.toSet());
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return Collections.emptySet();
	}
	
	private void crawlSubCategoryPage(final String url) {
		System.out.println("[SamsClubCrawlerStrategy] - Attempting to crawl sub category page");
		visitedUrls.add(url);
		driver.get(url);
		try {
			parseProductsOnCurrentPage();
			
			driver.setImplicitWait(1);
			try {
				System.out.println("[SamsClubCrawlerStrategy] - Navigating to next page of products...");
				final String nextButtonSelector = ".sc-pagination-next > a:nth-child(1)";
				driver.findElement(By.cssSelector(nextButtonSelector));
				System.out.println("\tsuccess");
				driver.js("document.querySelector(\""+nextButtonSelector+"\").click();");
				driver.sleep(3000); //sluggish sams club front end
				parseProductsOnCurrentPage();
			} catch(final Exception e) {
				System.out.println("\tfailure");
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	private void parseProductsOnCurrentPage() {
		System.out.println("[SamsClubCrawlerStrategy] - Parsing products on current page");
		final Set<String> links = parseAllLinksOnPage();
		visitedUrls.addAll(links);
		
		for(final String url : links) {
			if(getPageTypeForUrl(url) == PageType.PRODUCT) {
				System.out.println("[SamsClubCrawlerStrategy] - Found product url: " + url);
				urlFound(url);
			}
		}	
	}
	
	private String trimUrl(final String url) {
		if(url == null) {
			return null;
		}
		
		final int queryStart = url.indexOf("?");
		return queryStart > -1 ? url.substring(0, queryStart) : url;
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
