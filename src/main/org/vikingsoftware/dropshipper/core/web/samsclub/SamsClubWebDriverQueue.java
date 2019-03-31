package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.SamsClubDriverSupplier;

public class SamsClubWebDriverQueue {

	private static final int SELENIUM_INSTANCES_PER_CORE = 2;

	protected final ExecutorService threadPool;
	protected final LinkedBlockingDeque<SamsClubDriverSupplier> webDrivers;

	protected SamsClubWebDriverQueue() {
		final int coresAvailable = Runtime.getRuntime().availableProcessors();
		final int numThreads = coresAvailable * SELENIUM_INSTANCES_PER_CORE;
		System.out.println("Using " + numThreads + " threads for SamsClubWebDriverQueue");
		threadPool = Executors.newFixedThreadPool(numThreads);

		webDrivers = new LinkedBlockingDeque<>(numThreads);
		for(int i = 0; i < numThreads; i++) {
			try {
				webDrivers.put(new SamsClubDriverSupplier());
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
