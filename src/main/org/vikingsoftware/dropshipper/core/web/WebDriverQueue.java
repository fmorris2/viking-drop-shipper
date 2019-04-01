package main.org.vikingsoftware.dropshipper.core.web;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

import org.openqa.selenium.WebDriver;

import main.org.vikingsoftware.dropshipper.core.utils.ThreadUtils;

public class WebDriverQueue<T extends WebDriver> {

	protected final LinkedBlockingDeque<DriverSupplier<T>> webDrivers;

	public WebDriverQueue(final Supplier<DriverSupplier<T>> driverSupplier) {
		webDrivers = new LinkedBlockingDeque<>(ThreadUtils.NUM_THREADS);
		for(int i = 0; i < /*ThreadUtils.NUM_THREADS*/2; i++) {
			try {
				webDrivers.put(driverSupplier.get());
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized DriverSupplier<T> request() {
		try {
			final DriverSupplier<T> supp = webDrivers.take();
			System.out.println("Request " + supp + " on " + this + ": There are now " + webDrivers.size() + " drivers in the queue");
			return supp;
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public void relinquish(final DriverSupplier<?> supplier) {
		try {
			System.out.println("Attempting to reqlinquish " + supplier);
			webDrivers.addFirst((DriverSupplier<T>)supplier);
			System.out.println("Relinquish " + supplier + " on " + this + ": There are now " + webDrivers.size() + " drivers in the queue");
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
}
