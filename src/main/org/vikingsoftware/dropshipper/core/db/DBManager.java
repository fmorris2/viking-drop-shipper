package main.org.vikingsoftware.dropshipper.core.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


public abstract class DBManager {
	private static final int VALIDITY_CHECK_TIMEOUT = 5000;
	
	private final String DATABASE_URL;
	private final Properties CONNECTION_PROPERTIES;
	
	private Connection connection;
	
	protected DBManager() {
		DATABASE_URL = "jdbc:"+getDBType()+"://"+getDBHost()+":"+getDBPort()+"/"+getDBName();
		CONNECTION_PROPERTIES = new Properties();
		CONNECTION_PROPERTIES.put("user", getDBUser());
		CONNECTION_PROPERTIES.put("password", getUserPass());
	}
	
	protected abstract String getDBHost();
	protected abstract String getDBPort();
	protected abstract String getDBName();
	protected abstract String getDBUser();
	protected abstract String getUserPass();
	protected abstract String getDBType();
	
	public Statement createStatement() {
		try {
			if(!isConnectionValid()) {
				refreshConnection();
			}	
			return connection.createStatement();
		}catch(final SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void refreshConnection() throws SQLException {
		connection = DriverManager.getConnection(DATABASE_URL, CONNECTION_PROPERTIES);
	}
	
	private boolean isConnectionValid() throws SQLException {
		return connection != null && connection.isValid(VALIDITY_CHECK_TIMEOUT);
	}
	
}
