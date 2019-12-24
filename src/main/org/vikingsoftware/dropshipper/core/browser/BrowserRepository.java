package main.org.vikingsoftware.dropshipper.core.browser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.WebDriverQueue;

public final class BrowserRepository {

	private static BrowserRepository instance;
	private static Object instanceLock = new Object();

	private final Map<Class<? extends DriverSupplier<?>>, WebDriverQueue<? extends WebDriver>> queueCache = new ConcurrentHashMap<>();

	private BrowserRepository() {
		queueCache.put(AliExpressDriverSupplier.class, new WebDriverQueue<>(() -> new AliExpressDriverSupplier()));
		queueCache.put(SamsClubDriverSupplier.class, new WebDriverQueue<>(() -> new SamsClubDriverSupplier()));
		queueCache.put(CostcoDriverSupplier.class, new WebDriverQueue<>(() -> new CostcoDriverSupplier()));
	}

	public static BrowserRepository get() {
		synchronized(instanceLock) {
			if(instance == null) {
				instance = new BrowserRepository();
			}
	
			return instance;
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends DriverSupplier<?>> T request(final Class<?> type) {
		System.out.println("[BrowserRepository] - Request driver supplier of type " + type);
		final WebDriverQueue<?> queue = queueCache.get(type);
		if(queue != null) {
			System.out.println("\trequesting...");
			final T supplier = (T)queue.request();
			System.out.println("\tsuccessfully requested driver supplier.");
			return supplier;
		}

		return null;
	}

	public synchronized void relinquish(final DriverSupplier<?> supplier) {
		final WebDriverQueue<?> queue = queueCache.get(supplier.getClass());
		if(queue != null) {
			queue.relinquish(supplier);
		}
	}

	public synchronized void replace(final Class<? extends DriverSupplier<?>> supplierClass) {
		System.out.println("BrowserRepository#replace("+supplierClass+")");
		final WebDriverQueue<?> queue = queueCache.get(supplierClass);
		if(queue != null) {
			System.out.println("Queue is not null for class " + supplierClass);
			queue.replace(supplierClass);
		}
	}

	public synchronized void replaceAll() {
		System.out.println("Replacing all WebDrivers in BrowserRepository.");
		for(final WebDriverQueue<? extends WebDriver> queue : queueCache.values()) {
			queue.replaceAll();
		}
	}

}
