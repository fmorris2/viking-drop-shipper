package main.org.vikingsoftware.dropshipper;

import java.io.IOException;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.data.fulfillment.FulfillmentAccountManager;
import main.org.vikingsoftware.dropshipper.core.web.LoginWebDriver;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;

public class VSDropShipper {

	public static final String VS_PHONE_NUM = "9162450125";
	
	private static final CycleParticipant[] MAIN_THREAD_MODULES = {
//		new OrderParser(),
		new OrderExecutor(),
//		new MarginAdjuster(),
//		new OrderTracking(),
//		new TrackingHistoryUpdater(),
//		new EbayAccountActivityFees()	
	};

	public static void main(final String[] args) throws InterruptedException, IOException {
		//killProcesses();
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
				
				FulfillmentAccountManager.get().load();
				LoginWebDriver.clearSessionCaches();
				//killProcesses();
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void killProcesses() throws IOException {
		Runtime.getRuntime().exec("pkill -9 firefox");
		Runtime.getRuntime().exec("pkill -9 geckodriver");
	}
}
