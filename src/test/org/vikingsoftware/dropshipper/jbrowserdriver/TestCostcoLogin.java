package test.org.vikingsoftware.dropshipper.jbrowserdriver;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.web.DefaultWebDriver;

public class TestCostcoLogin {

	@Test
	public void test() {
		final WebDriver driver = new DefaultWebDriver();
		driver.get("https://www.costco.com/LogonForm");
	}

}
