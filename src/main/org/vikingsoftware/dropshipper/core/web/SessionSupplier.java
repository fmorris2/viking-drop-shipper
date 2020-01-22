package main.org.vikingsoftware.dropshipper.core.web;

import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;

public interface SessionSupplier {
	
	public Map<String, String> getSession(final FulfillmentAccount account, final VSDSProxy proxy);
	public void clearSession(final FulfillmentAccount account);
}
