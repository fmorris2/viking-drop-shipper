package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;
import main.org.vikingsoftware.dropshipper.core.utils.DBLogging;

public class FulfillmentAccountManager {

	private static FulfillmentAccountManager instance;

	private final Map<FulfillmentPlatforms, LinkedList<FulfillmentAccount>> accounts = new ConcurrentHashMap<>();

	private FulfillmentAccountManager() {
		//intentionally empty
	}

	public static synchronized FulfillmentAccountManager get() {
		if(instance == null) {
			instance = new FulfillmentAccountManager();
			instance.load();
		}

		return instance;
	}

	public synchronized FulfillmentAccount getAndRotateEnabledAccount(final FulfillmentPlatforms platform) {
		return getAndRotateAccount(platform, true);
	}
	
	private synchronized FulfillmentAccount getAndRotateAccount(final FulfillmentPlatforms platform,
			final boolean enabled) {
		final FulfillmentAccount account = accounts.computeIfAbsent(platform, plat -> new LinkedList<>())
				.stream()
				.filter(acc -> acc.isEnabled() || !enabled)
				.findFirst()
				.orElse(null);
		
		if(account != null) {
			accounts.get(platform).remove(account);
			accounts.get(platform).add(account);
		}

		return account;
	}

	public FulfillmentAccount getAccountById(final int id) {
		for(final Map.Entry<FulfillmentPlatforms, LinkedList<FulfillmentAccount>> entry : accounts.entrySet()) {
			for(final FulfillmentAccount acc : entry.getValue()) {
				if(acc.id == id) {
					return acc;
				}
			}
		}

		return null;
	}
	
	public FulfillmentAccount getAccountByTransactionId(final String transactionId) {
		FulfillmentAccount acc = null;
		try(final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT fulfillment_account_id FROM processed_order "
					+ "WHERE fulfillment_transaction_id='"+transactionId+"'")) {
			if(res.next()) {
				acc = FulfillmentAccountManager.get().getAccountById(res.getInt("fulfillment_account_id"));
			}
			 
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return acc;
	} 
	
	public FulfillmentAccount peekEnabledAccount(final FulfillmentPlatforms platform) {
		return peekAccount(platform, true);
	}
	
	public void markAccountAsDisabled(final FulfillmentAccount account) {
		try(final Statement st = VSDSDBManager.get().createStatement()) {
			System.out.println("Marking account as disabled: " + account);
			st.execute("UPDATE fulfillment_account SET is_enabled=0 WHERE id="+account.id);
			account.setIsEnabled(false);
			System.out.println("\tsuccess.");
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getNumProcessedOrdersForAccount(final int id) {
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT COUNT(*) FROM processed_order WHERE fulfillment_account_id="+id)) {
			if(res.next()) {
				return res.getInt(1);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public LocalDateTime getMostRecentProcessedOrderForAccount(final int id) {
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT date_processed FROM processed_order WHERE "
					+ "fulfillment_account_id="+id + " ORDER BY date_processed DESC LIMIT 1")) {
			
			if(res.next()) {
				final long timestamp = res.getLong(1);
				return LocalDateTime.from(Instant.ofEpochMilli(timestamp));
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private FulfillmentAccount peekAccount(final FulfillmentPlatforms platform, final boolean enabled) {
		System.out.println("Peeking fulfillment account for platform " + platform);
		final FulfillmentAccount account = accounts.computeIfAbsent(platform, plat -> new LinkedList<>())
			.stream()
			.filter(acc -> acc.isEnabled() || !enabled)
			.findFirst()
			.orElse(null);
		
		System.out.println("\tAccount: " + account);	
		return account;
	}

	public void load() {
		accounts.clear();
		try (
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT * FROM fulfillment_account ORDER BY id ASC")) {
			while(res.next()) {
				final int id = res.getInt("id");
				final int plat_id = res.getInt("fulfillment_platform_id");
				final String username = res.getString("username");
				final String password = res.getString("password");
				final boolean is_enabled = res.getBoolean("is_enabled");
				final FulfillmentPlatforms platform = FulfillmentPlatforms.getById(plat_id);
				final FulfillmentAccount account = new FulfillmentAccount(id, plat_id, username, password, is_enabled);
				final LinkedList<FulfillmentAccount> queue = accounts.getOrDefault(platform, new LinkedList<>());
				queue.add(account);
				accounts.put(platform, queue);
			}
		} catch(final Exception e) {
			DBLogging.critical(getClass(), "Failed to load fulfillment accounts!", e);
		}
	}
}
