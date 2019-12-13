package main.org.vikingsoftware.dropshipper.core.ebay;

import main.org.vikingsoftware.dropshipper.core.CycleParticipant;
import main.org.vikingsoftware.dropshipper.tools.FillTransactionTable;

public class EbayAccountActivityFees implements CycleParticipant {
	
	private static final int DAYS_IN_PAST = 2;
	private static final long CYCLE_COOLDOWN = 60_000 * 30; //we'll parse account activity every 30 minutes
	
	private long lastCycleTime;
		
	@Override
	public void cycle() {
		System.out.println("Parsing eBay account activity fees (insertion fees & store subscription fees)");
		FillTransactionTable.insertAllAccountTransactions(DAYS_IN_PAST);
		lastCycleTime = System.currentTimeMillis();
	}
	
	@Override
	public boolean shouldCycle() {
		return System.currentTimeMillis() - lastCycleTime >= CYCLE_COOLDOWN;
	}

}
