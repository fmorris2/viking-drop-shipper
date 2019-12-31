package main.org.vikingsoftware.dropshipper.core.web;

import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;

public interface SessionSupplier {
	
	public Map<String, String> getSession(final FulfillmentAccount account);
	public void clearSession();
}
