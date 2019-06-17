package main.org.vikingsoftware.dropshipper;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.core.browser.BrowserRepository;
import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;
import main.org.vikingsoftware.dropshipper.inventory.InventoryUpdater;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.parser.OrderParser;
import main.org.vikingsoftware.dropshipper.order.tracking.OrderTracking;

public class VSDropShipper {

	public static final String VS_PHONE_NUM = "4238454649";
	private static final long CYCLE_TIME_MS = 300_000;

	private static final CycleParticipant[] MODULES = {
		new OrderParser(),
		new OrderExecutor(),
		new InventoryUpdater(),
		new OrderTracking()
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
				SkuMappingManager.clear();
				BrowserRepository.get().replaceAll();
			} catch(final Exception e) {
				e.printStackTrace();
			}
			System.out.println("Sleeping between cycles...");
			Thread.sleep(CYCLE_TIME_MS);
		}
	}

}
