package main.org.vikingsoftware.dropshipper.core.db.impl;

import main.org.vikingsoftware.dropshipper.core.db.DBCredentialManager;
import main.org.vikingsoftware.dropshipper.core.db.DBManager;

public class VDSDBManager extends DBManager {

	private static VDSDBManager manager;

	public static VDSDBManager get() {
		if(manager == null) {
			manager = new VDSDBManager();
		}

		return manager;
	}

	@Override
	protected String getDBHost() {
		return "149.56.140.7";
	}

	@Override
	protected String getDBPort() {
		return "3306";
	}

	@Override
	protected String getDBName() {
		return DBCredentialManager.getDB();
	}

	@Override
	protected String getDBUser() {
		return DBCredentialManager.getUser();
	}

	@Override
	protected String getUserPass() {
		return DBCredentialManager.getPass();
	}

	@Override
	protected String getDBType() {
		return "mysql";
	}
}

