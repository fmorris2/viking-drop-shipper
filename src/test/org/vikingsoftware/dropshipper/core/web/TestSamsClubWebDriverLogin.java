package test.org.vikingsoftware.dropshipper.core.web;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.core.web.samsclub.SamsClubWebDriver;

public class TestSamsClubWebDriverLogin {

	@Test
	public void testSingleLogin() {
		Assert.assertTrue(new SamsClubWebDriver().getReady());
	}

	@Test
	public void testMultipleLogin() {
		testSingleLogin();
		testSingleLogin();
	}

}
