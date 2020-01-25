package main.org.vikingsoftware.dropshipper.core.net.proxy;

import java.util.HashMap;
import java.util.Map;

import main.org.vikingsoftware.dropshipper.core.utils.CircularQueue;

public class ProxySourceCooldownManager {
	
	/*
	 * if there were this many failed attempts in the specified time window,
	 * we will initial a cooldown
	 */
	private static final int FAILED_ATTEMPTS_THRESHOLD = 5;
	
	/* 
	 * if there were FAILED_ATTEMPTS_THRESHOLD failed attempts in this time window,
	 *  we need a cooldown
	 */
	private static final long FAILED_ATTEMPTS_TIME_THRESHOLD_MS = 60_000;

	private static final Map<VSDSProxySource, Long> ENFORCED_COOLDOWN_TIME_WINDOWS = new HashMap<>();
	
	static {
		ENFORCED_COOLDOWN_TIME_WINDOWS.put(VSDSProxySource.NORD, 60_000L * 2); //2 mins for NORD
	}
	
	private final Map<VSDSProxySource, Long> currentCooldownWindowStartTimes = new HashMap<>();
	private final Map<VSDSProxySource, CircularQueue<Long>> failedConnectionAttempts = new HashMap<>();
	
	public boolean isOnCooldown(final VSDSProxySource source) {
		final long currentCooldownWindowStart = currentCooldownWindowStartTimes.computeIfAbsent(source, s -> -1L);
		if(currentCooldownWindowStart > 0) {
			if(System.currentTimeMillis() - currentCooldownWindowStart <= ENFORCED_COOLDOWN_TIME_WINDOWS.get(source)) {
				return true;
			} else {
				currentCooldownWindowStartTimes.put(source, -1L);
				return false;
			}
		}
		
		return false;
	}
	
	public void reportFailedConnectionAttempt(final VSDSProxySource source) {
		final CircularQueue<Long> failedAttempts = failedConnectionAttempts.computeIfAbsent(source, s -> new CircularQueue<>(FAILED_ATTEMPTS_THRESHOLD));
		failedAttempts.add(System.currentTimeMillis());
		if(System.currentTimeMillis() - failedAttempts.getFirst() <= FAILED_ATTEMPTS_TIME_THRESHOLD_MS) {
			triggerCooldown(source);
		}
	}
	
	public void triggerCooldown(final VSDSProxySource source) {
		currentCooldownWindowStartTimes.put(source, System.currentTimeMillis());
	}
}
