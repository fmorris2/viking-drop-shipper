package main.org.vikingsoftware.dropshipper.core.web.costco;

import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;

public class CostcoWebDriver extends LoginWebDriver {

	@Override
	protected boolean prepareForExecutionViaLoginImpl() {
		return false;
	}

	@Override
	protected String getLandingPageURL() {
		return null;
	}

	@Override
	protected boolean verifyLoggedIn() {
		return false;
	}

}
