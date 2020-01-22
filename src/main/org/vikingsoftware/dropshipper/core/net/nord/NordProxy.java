package main.org.vikingsoftware.dropshipper.core.net.nord;

import main.org.vikingsoftware.dropshipper.core.net.VSDSProxy;

public class NordProxy extends VSDSProxy {

	public NordProxy(final String host, final int port, final String user, final String pass) {
		super(host, port, user, pass);
	}
	
	@Override
	public boolean supportsSocks() {
		return true;
	}

}
