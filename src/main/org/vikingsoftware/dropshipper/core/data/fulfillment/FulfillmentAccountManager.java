package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class FulfillmentAccountManager {

	private static FulfillmentAccountManager instance;

	private final Map<FulfillmentPlatforms, Queue<FulfillmentAccount>> accounts = new ConcurrentHashMap<>();

	private FulfillmentAccountManager() {
		//intentionally empty
	}

	public static FulfillmentAccountManager get() {
		if(instance == null) {
			instance = new FulfillmentAccountManager();
			instance.load();
		}

		return instance;
	}

	public synchronized FulfillmentAccount getAndRotateAccount(final FulfillmentPlatforms platform) {
		final FulfillmentAccount account = accounts.computeIfAbsent(platform, plat -> new PriorityQueue<>()).poll();
		if(account != null) {
			accounts.get(platform).add(account);
		}

		return account;
	}

	public FulfillmentAccount getAccountById(final int id) {
		for(final Map.Entry<FulfillmentPlatforms, Queue<FulfillmentAccount>> entry : accounts.entrySet()) {
			for(final FulfillmentAccount acc : entry.getValue()) {
				if(acc.id == id) {
					return acc;
				}
			}
		}

		return null;
	}

	public FulfillmentAccount peekAccount(final FulfillmentPlatforms platform) {
		return accounts.computeIfAbsent(platform, plat -> new PriorityQueue<>()).peek();
	}

	private void load() {
		try {
			final Statement st = VDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT * FROM fulfillment_accounts WHERE is_enabled = 1");
			while(res.next()) {
				final int id = res.getInt("id");
				final int plat_id = res.getInt("fulfillment_platform_id");
				final String username = res.getString("username");
				final String password = res.getString("password");
				final FulfillmentPlatforms platform = FulfillmentPlatforms.getById(plat_id);
				final FulfillmentAccount account = new FulfillmentAccount(id, plat_id, username, password);
				final Queue<FulfillmentAccount> queue = accounts.getOrDefault(platform, new PriorityQueue<>());
				queue.add(account);
				accounts.put(platform, queue);
			}
		} catch(final Exception e) {
			DBLogging.critical(getClass(), "Failed to load fulfillment accounts!", e);
		}
	}
}
