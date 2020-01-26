package main.org.vikingsoftware.dropshipper.tools;

import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import main.org.vikingsoftware.dropshipper.core.db.impl.VSDSDBManager;

public class FixDatesInProcessedOrder {

	private static final DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
	public static void main(String[] args) {
		try(final Statement st = VSDSDBManager.get().createStatement();
		    final ResultSet res = st.executeQuery("SELECT id,date_processed FROM processed_order")) {
			
			while(res.next()) {
				final int id = res.getInt("id");
				final String dateProcessed = Long.toString(res.getLong("date_processed"));
				
				if(dateProcessed.startsWith("2019")) { //need to fix
					final Date convertedToDate = format.parse(dateProcessed);
					final long convertedToMs = convertedToDate.getTime();
					VSDSDBManager.get().createStatement().execute("UPDATE processed_order SET date_processed="+convertedToMs + " WHERE id="+id);
				}
			}
			
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

}
