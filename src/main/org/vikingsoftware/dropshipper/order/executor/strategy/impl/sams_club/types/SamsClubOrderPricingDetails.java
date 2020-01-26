package main.org.vikingsoftware.dropshipper.order.executor.strategy.impl.sams_club.types;

import org.json.JSONObject;

public class SamsClubOrderPricingDetails {
	
	public final double total;
	public final double subTotal;
	public final double salesTax;
	public final double productFees;
	public final double savings;
	public final double shipping;
	
	public double profit;
	
	public SamsClubOrderPricingDetails(final JSONObject json) {
		total = json.getDouble("total");
		subTotal = json.getDouble("subTotal");
		salesTax = json.getDouble("salesTax");
		productFees = json.getDouble("productFee");
		shipping = json.getDouble("shippingAmount");
		savings = json.getDouble("savings");
	}
}
