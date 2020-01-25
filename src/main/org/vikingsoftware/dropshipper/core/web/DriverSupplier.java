package main.org.vikingsoftware.dropshipper.core.web;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import main.org.vikingsoftware.dropshipper.core.net.proxy.VSDSProxy;

public abstract class DriverSupplier<T> implements Function<VSDSProxy, T> {
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	
	public T get() {
		return apply(null);
	}
	
	@Override
	public T apply(final VSDSProxy proxy) {
		lock.writeLock().lock();
		try {
			return internalGet(proxy);
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	protected abstract T internalGet(final VSDSProxy proxy);

}
