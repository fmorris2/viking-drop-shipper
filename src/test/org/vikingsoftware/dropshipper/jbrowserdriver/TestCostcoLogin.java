package test.org.vikingsoftware.dropshipper.jbrowserdriver;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

public class TestCostcoLogin {

	@Test
	public void test() {
		final WebDriver driver = new JBrowserDriver();
		driver.get("https://www.costco.com/LogonForm");
	}

}
