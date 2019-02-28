package test.org.vikingsoftware.dropshipper.inventory;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.inventory.InventoryUpdater;

import org.junit.Test;

public class TestInventoryUpdater {

	@Test
	public void test() {
		final InventoryUpdater updater = new InventoryUpdater();
		for(int i = 0; i < 1000; i++) {
			updater.cycle();
		}
		Assert.assertTrue(true);
	}

}
