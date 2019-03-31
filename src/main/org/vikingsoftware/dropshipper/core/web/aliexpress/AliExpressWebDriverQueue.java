package main.org.vikingsoftware.dropshipper.core.web.aliexpress;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.stock.impl.AliExpressDriverSupplier;

public abstract class AliExpressWebDriverQueue {

	private static final int SELENIUM_INSTANCES_PER_CORE = 3;

	protected final ExecutorService threadPool;
	protected final BlockingQueue<AliExpressDriverSupplier> webDrivers;

	protected AliExpressWebDriverQueue() {
		final int coresAvailable = Runtime.getRuntime().availableProcessors();
		final int numThreads = coresAvailable * SELENIUM_INSTANCES_PER_CORE;
		System.out.println("Using " + numThreads + " threads for AliExpressWebDriverQueue");
		threadPool = Executors.newFixedThreadPool(numThreads);

		webDrivers = new ArrayBlockingQueue<>(numThreads);
		for(int i = 0; i < numThreads; i++) {
			try {
				webDrivers.put(new AliExpressDriverSupplier());
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
