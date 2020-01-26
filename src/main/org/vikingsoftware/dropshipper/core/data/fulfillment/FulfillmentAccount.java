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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fulfillment_platform_id;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FulfillmentAccount other = (FulfillmentAccount) obj;
		if (fulfillment_platform_id != other.fulfillment_platform_id)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	
}
