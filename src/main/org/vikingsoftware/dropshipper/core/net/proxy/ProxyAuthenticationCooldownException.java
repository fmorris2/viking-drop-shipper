package main.org.vikingsoftware.dropshipper.core.net.proxy;

public class ProxyAuthenticationCooldownException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public ProxyAuthenticationCooldownException(final Exception e) {
		super(e);
	}

}
