package test.org.vikingsoftware.dropshipper.core.data;

import main.org.vikingsoftware.dropshipper.core.data.sku.SkuMappingManager;

import org.junit.Assert;
import org.junit.Test;

public class TestSkuMappingManager {

	@Test
	public void test() {
		Assert.assertTrue(SkuMappingManager.load());
	}

}
