package main.org.vikingsoftware.dropshipper.core.web.samsclub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.json.JSONObject;

import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccount;
import main.org.vikingsoftware.dropshipper.core.net.http.WrappedHttpClient;
import main.org.vikingsoftware.dropshipper.core.web.SessionSupplier;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.requests.SamsClubLoginRequest;

public final class SamsClubSessionProvider implements SessionSupplier<SamsClubLoginResponse> {
	
	private static SamsClubSessionProvider instance;
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Map<FulfillmentAccount, SamsClubLoginResponse> sessionCache = new HashMap<>();
	
	private SamsClubSessionProvider() {
		//singleton
	}
	
	public static synchronized SamsClubSessionProvider get() {
		if(instance == null) {
			instance = new SamsClubSessionProvider();
		}
		
		return instance;
	}

	@Override
	public SamsClubLoginResponse getSession(final FulfillmentAccount account, final WrappedHttpClient client) {
		lock.writeLock().lock();
		try {
			SamsClubLoginResponse currentSession = sessionCache.computeIfAbsent(account, acc -> null);
			if(currentSession == null) {
				final SamsClubLoginRequest request = new SamsClubLoginRequest(account, client);
				final Optional<JSONObject> response = request.execute();
				if(response.isPresent()) {
					final Map<String, String> cookies = request.getCookieMap();
					currentSession = new SamsClubLoginResponse(cookies, response.get());
					sessionCache.put(account, currentSession);
				}
			}
			
			return currentSession;
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void clearSession(final FulfillmentAccount account) {
		lock.writeLock().lock();
		try {
			sessionCache.put(account, null);
		} finally {
			lock.writeLock().unlock();
		}
	}

}
