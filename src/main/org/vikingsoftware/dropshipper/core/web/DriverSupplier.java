package main.org.vikingsoftware.dropshipper.core.web;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public abstract class DriverSupplier<T> implements Supplier<T> {
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	@Override
	public T get() {
		lock.writeLock().lock();
		try {
			return internalGet();
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	protected abstract T internalGet();

}
