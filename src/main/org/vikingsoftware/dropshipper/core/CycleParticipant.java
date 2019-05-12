package main.org.vikingsoftware.dropshipper.core;

import java.sql.ResultSet;
import java.sql.Statement;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public interface CycleParticipant {
	
	public void cycle();
	
	default boolean shouldCycle() {
		try {
			final Statement st = VSDSDBManager.get().createStatement();
			final ResultSet res = st.executeQuery("SELECT power_switch FROM global_settings LIMIT 1");
			if(res.next()) {
				return res.getBoolean(1);
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
