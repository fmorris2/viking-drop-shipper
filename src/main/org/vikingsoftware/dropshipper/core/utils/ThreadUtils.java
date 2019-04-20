package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {

	public static final int THREADS_PER_CORE = 1;
	public static final int NUM_THREADS = calculateThreads();

	public static final ExecutorService threadPool = Executors.newFixedThreadPool(calculateThreads());

	private static int calculateThreads() {
		final int coresAvailable = Runtime.getRuntime().availableProcessors();
		return coresAvailable * THREADS_PER_CORE;
	}
}
