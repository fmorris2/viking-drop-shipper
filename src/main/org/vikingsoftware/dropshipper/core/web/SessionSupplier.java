package main.org.vikingsoftware.dropshipper.core.web;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;

public interface SessionSupplier<T> {
	
	public T getSession(final FulfillmentAccount account, final WrappedHttpClient client);
	public void clearSession(final FulfillmentAccount account);
}
