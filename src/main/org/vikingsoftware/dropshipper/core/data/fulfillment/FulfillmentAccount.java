package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

public class FulfillmentAccount implements Comparable<FulfillmentAccount> {
	public final int id;
	public final int fulfillment_platform_id;
	public final String username;
	public final String password;
	public final boolean is_enabled;

	public FulfillmentAccount(final int id, final int platform_id, final String username,
			final String password, final boolean is_enabled) {
		this.id = id;
		this.fulfillment_platform_id = platform_id;
		this.username = username;
		this.password = password;
		this.is_enabled = is_enabled;
	}

	@Override
	public int compareTo(FulfillmentAccount other) {
		return 0;
	}
}
