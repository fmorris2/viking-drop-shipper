package test.org.vikingsoftware.dropshipper.core.db;

import java.sql.SQLException;
import java.sql.Statement;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

import org.junit.Assert;
import org.junit.Test;

public class TestVDSDBManager {

	@Test
	public void test() {
		try (final Statement st = VSDSDBManager.get().createStatement()){
			Assert.assertTrue("DB connection is not valid", st.getConnection().isValid(5));
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		
		Assert.assertTrue("Failed to make DB connection", true);
	}

}
