package main.org.vikingsoftware.dropshipper.core.data.fulfillment;

import main.org.vikingsoftware.dropshipper.order.executor.strategy.OrderExecutionStrategy;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.AliExpressOrderExecutionStrategy;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.CostcoOrderExecutionStrategy;
import main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.SamsClubOrderExecutionStrategy;

public enum FulfillmentPlatforms {
	ALI_EXPRESS(AliExpressOrderExecutionStrategy.class),
	SAMS_CLUB(SamsClubOrderExecutionStrategy.class),
	AMAZON(null),
	COSTCO(CostcoOrderExecutionStrategy.class),
	WALMART(null),
	BJS(null),
	;

	private final Class<? extends OrderExecutionStrategy> strategy;
	FulfillmentPlatforms(final Class<? extends OrderExecutionStrategy> strategy) {
		this.strategy = strategy;
	}

	public static FulfillmentPlatforms getById(final int id) {
		return values()[id - 1];
	}

	public OrderExecutionStrategy generateStrategy() {
		try {
			return strategy == null ? null : strategy.newInstance();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public int getId() {
		return ordinal() + 1;
	}
}
