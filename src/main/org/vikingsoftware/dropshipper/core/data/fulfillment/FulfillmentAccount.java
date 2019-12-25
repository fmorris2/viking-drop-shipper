package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

public class FulfillmentAccount implements Comparable<FulfillmentAccount> {
	public final int id;
	public final int fulfillment_platform_id;
	public final String username;
	public final String password;
	
	private boolean is_enabled;

	public FulfillmentAccount(final int id, final int platform_id, final String username,
			final String password, final boolean is_enabled) {
		this.id = id;
		this.fulfillment_platform_id = platform_id;
		this.username = username;
		this.password = password;
		this.is_enabled = is_enabled;
	}
	
	public boolean isEnabled() {
		return is_enabled;
	}
	
	public void setIsEnabled(final boolean isEnabled) {
		is_enabled = false;
	}
	
	@Override
	public String toString() {
		return "id: " + id + ", username: " + username + ", password: " + password;
	}

	@Override
	public int compareTo(FulfillmentAccount other) {
		return 0;
	}
}
