package main.org.vikingsoftware.dropshipper;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.ebay.EbayAccountActivityFees;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.inventory.InventoryUpdater;
import main.org.vikingsoftware.dropshipper.order.parser.OrderParser;
import main.org.vikingsoftware.dropshipper.order.tracking.OrderTracking;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryUpdater;
import main.org.vikingsoftware.dropshipper.pricing.margins.MarginAdjuster;

public class VSDropShipper {

	public static final String VS_PHONE_NUM = "9162450125";
	
	private static final CycleParticipant[] MAIN_THREAD_MODULES = {
		new OrderParser(),
		new InventoryUpdater(),
		new MarginAdjuster(),
		new InventoryUpdater(),
		new OrderTracking(),
		new TrackingHistoryUpdater(),
		new EbayAccountActivityFees()	
	};

	public static void main(final String[] args) throws InterruptedException {
		cycle();
	}
	
	private static void cycle() {
		while(true) {
			try {
				System.out.println("Cycling...");
				for(final CycleParticipant module : MAIN_THREAD_MODULES) {
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
