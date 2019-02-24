package main.org.vikingsoftware.dropshipper;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.order.executor.OrderExecutor;
import main.org.vikingsoftware.dropshipper.order.parser.OrderParser;

public class VSDropShipper {

	private static final long CYCLE_TIME_MS = 60_000;
	
	private static final CycleParticipant[] MODULES = {
		new OrderParser(),
		new OrderExecutor()
	};
	
	public static void main(final String[] args) throws InterruptedException {
		while(true) {
			
			for(final CycleParticipant module : MODULES) {
				System.out.println("Executing module: " + module);
				try {
					module.cycle();
				} catch(final Exception e) {
					e.printStackTrace();
					System.out.println("Failed to execute module: " + module);
				}
			}
			
			Thread.sleep(CYCLE_TIME_MS);
		}
		
	}

}
