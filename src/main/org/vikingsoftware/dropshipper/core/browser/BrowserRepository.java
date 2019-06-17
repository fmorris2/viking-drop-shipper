package main.org.vikingsoftware.dropshipper.core.browser;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.WebDriverQueue;

public final class BrowserRepository {

	private static BrowserRepository instance;

	private final Map<Class<? extends DriverSupplier<?>>, WebDriverQueue<? extends WebDriver>> queueCache = new HashMap<>();

	private BrowserRepository() {
		queueCache.put(AliExpressDriverSupplier.class, new WebDriverQueue<>(() -> new AliExpressDriverSupplier()));
		queueCache.put(SamsClubDriverSupplier.class, new WebDriverQueue<>(() -> new SamsClubDriverSupplier()));
		queueCache.put(CostcoDriverSupplier.class, new WebDriverQueue<>(() -> new CostcoDriverSupplier()));
	}

	public synchronized static BrowserRepository get() {
		if(instance == null) {
			instance = new BrowserRepository();
		}

		return instance;
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends DriverSupplier<?>> T request(final Class<?> type) {
		final WebDriverQueue<?> queue = queueCache.get(type);
		if(queue != null) {
			return (T)queue.request();
		}

		return null;
	}

	public void relinquish(final DriverSupplier<?> supplier) {
		if(supplier == null) {
			return;
		}

		final WebDriverQueue<?> queue = queueCache.get(supplier.getClass());
		if(queue != null) {
			queue.relinquish(supplier);
		}
	}

	public void replace(final Class<? extends DriverSupplier<?>> supplierClass) {
		final WebDriverQueue<?> queue = queueCache.get(supplierClass);
		if(queue != null) {
			queue.replace(supplierClass);
		}
	}

	public void replaceAll() {
		System.out.println("Replacing all WebDrivers in BrowserRepository.");
		for(final WebDriverQueue<? extends WebDriver> queue : queueCache.values()) {
			queue.replaceAll();
		}
	}

}
