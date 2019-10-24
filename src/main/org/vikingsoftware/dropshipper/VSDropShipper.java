package main.org.vikingsoftware.dropshipper;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.inventory.InventoryUpdater;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.parser.OrderParser;
import main.org.vikingsoftware.dropshipper.order.tracking.OrderTracking;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryUpdater;
import main.org.vikingsoftware.dropshipper.pricing.margins.MarginAdjuster;

public class VSDropShipper {

	public static final String VS_PHONE_NUM = "4238454649";

	private static final CycleParticipant[] MODULES = {
		new OrderParser(),
		new OrderExecutor(),
		//new MarginAdjuster(),
		new InventoryUpdater(),
		new OrderTracking(),
		new TrackingHistoryUpdater()
	};

	public static void main(final String[] args) throws InterruptedException {
		while(true) {
			System.out.println("Cycling...");
			for(final CycleParticipant module : MODULES) {
				if(!module.shouldCycle()) {
					continue;
				}
				System.out.println("Executing module: " + module);
				try {
					module.cycle();
				} catch(final Exception e) {
					e.printStackTrace();
					System.out.println("Failed to execute module: " + module);
				}
			}

			try {
				BrowserRepository.get().replaceAll();
				LoginWebDriver.clearSessionCaches();
				Runtime.getRuntime().exec("pkill -9 firefox");
				Runtime.getRuntime().exec("pkill -9 geckodriver");
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
	}

}
