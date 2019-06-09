package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;

public class RecentSalesRenderer extends JBrowserDriver {
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public RecentSalesRenderer() {
		super(new Settings.Builder()
				.headless(false)
				.build()
		);
	}
	
	@Override
	public void get(String url) {
		executor.execute(() -> {
			super.get(url);
		});
	}
}
