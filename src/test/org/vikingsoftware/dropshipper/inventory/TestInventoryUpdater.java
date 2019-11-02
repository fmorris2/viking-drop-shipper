package test.org.vikingsoftware.dropshipper.inventory;

import org.junit.Test;

import junit.framework.Assert;
import main.org.vikingsoftware.dropshipper.inventory.InventoryUpdater;

public class TestInventoryUpdater {

	@Test
	public void test() {
		final InventoryUpdater updater = new InventoryUpdater();
		updater.cycle();
		Assert.assertTrue(true);
	}

}
