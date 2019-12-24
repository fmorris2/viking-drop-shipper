package main.org.vikingsoftware.dropshipper.core.browser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.CostcoDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.DriverSupplier;
import main.org.vikingsoftware.dropshipper.core.web.WebDriverQueue;

public final class BrowserRepository {

	private static final Object instanceLock = new Object();
	
	private static BrowserRepository instance;

	private final Map<Class<? extends DriverSupplier<?>>, WebDriverQueue<? extends WebDriver>> queueCache = new HashMap<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
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
	public <T extends DriverSupplier<?>> T request(final Class<?> type) {
		lock.readLock().lock();
		try {
			System.out.println("[BrowserRepository] - Request driver supplier of type " + type);
			final WebDriverQueue<?> queue = queueCache.get(type);
			if(queue != null) {
				System.out.println("\trequesting...");
				final T supplier = (T)queue.request();
				System.out.println("\tsuccessfully requested driver supplier.");
				return supplier;
			}
		} finally {
			lock.readLock().unlock();
		}

		return null;
	}

	public void relinquish(final DriverSupplier<?> supplier) {
		lock.readLock().lock();
		try {
			final WebDriverQueue<?> queue = queueCache.get(supplier.getClass());
			if(queue != null) {
				queue.relinquish(supplier);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	public void replace(final Class<? extends DriverSupplier<?>> supplierClass) {
		lock.readLock().lock();
		try {
			System.out.println("BrowserRepository#replace("+supplierClass+")");
			final WebDriverQueue<?> queue = queueCache.get(supplierClass);
			if(queue != null) {
				System.out.println("Queue is not null for class " + supplierClass);
				queue.replace(supplierClass);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	public void replaceAll() {
		lock.readLock().lock();
		try {
			System.out.println("Replacing all WebDrivers in BrowserRepository.");
			for(final WebDriverQueue<? extends WebDriver> queue : queueCache.values()) {
				queue.replaceAll();
			}
		} finally {
			lock.readLock().unlock();
		}
	}

}
