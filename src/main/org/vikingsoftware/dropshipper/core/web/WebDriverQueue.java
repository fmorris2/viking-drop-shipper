package main.org.vikingsoftware.dropshipper.core.web;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.utils.ThreadUtils;

public class WebDriverQueue<T extends WebDriver> {

	protected final LinkedBlockingDeque<DriverSupplier<T>> webDrivers;

	private final Supplier<DriverSupplier<T>> driverSupplier;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

	public WebDriverQueue(final Supplier<DriverSupplier<T>> driverSupplier) {

		this.driverSupplier = driverSupplier;
		webDrivers = new LinkedBlockingDeque<>(ThreadUtils.NUM_THREADS);

		populateWebDrivers();
	}

	private void populateWebDrivers() {
		for(int i = 0; i < /*ThreadUtils.NUM_THREADS*/2; i++) {
			try {
				webDrivers.put(driverSupplier.get());
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public DriverSupplier<T> request() {
		lock.writeLock().lock();
		try {
			final DriverSupplier<T> supp = webDrivers.take();
			System.out.println("Request " + supp + " on " + this + ": There are now " + webDrivers.size() + " drivers in the queue");
			return supp;
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.writeLock().unlock();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public void relinquish(final DriverSupplier<?> supplier) {
		assert supplier != null;
		try {
			System.out.println("Attempting to reqlinquish " + supplier);
			webDrivers.addFirst((DriverSupplier<T>)supplier);
			System.out.println("Relinquish " + supplier + " on " + this + ": There are now " + webDrivers.size() + " drivers in the queue");
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	public void replace(final Class<? extends DriverSupplier<?>> supplier) {
		System.out.println("WebDriverQueue#replace("+supplier+")");
		assert supplier != null;
		try {
			System.out.println("Attempting to replace " + supplier);
			webDrivers.add(driverSupplier.get());
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	public void replaceAll() {
		for(final DriverSupplier<? extends WebDriver> driver : webDrivers) {
			driver.get().close();
		}
		webDrivers.clear();
		populateWebDrivers();
	}
}
