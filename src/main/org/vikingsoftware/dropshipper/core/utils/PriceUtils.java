package main.org.vikingsoftware.dropshipper.core.utils;

import java.util.function.Function;

public final class PriceUtils {
	
	private static final double SALES_TAX = .07;
	
	//9.15% Max Final Value Fee w/ Premium Store - Check this link: https://pages.ebay.com/seller-center/run-your-store/subscriptions-and-fees.html#m22_tb_a2__2 
	private static final Function<Double, Double> EBAY_FEE_FORMULA = sellPrice -> sellPrice * .0915;
	
	//PayPal charges a flat $.30 + 2.9% of the transaction total as a fee
	private static final Function<Double, Double> PAYPAL_FEE_FORMULA = sellPrice ->  0.30 + (sellPrice * .029);
	
	private PriceUtils() {}

	public static double getMarginPercentage(final double buyPrice, final double sellPrice) {
		final double buyPriceWithTax = buyPrice * (1D + SALES_TAX);
		final double eBayFinalValueFee = EBAY_FEE_FORMULA.apply(sellPrice);
		final double paypalFee = PAYPAL_FEE_FORMULA.apply(sellPrice);
		final double totalMoneySpent = (buyPriceWithTax + eBayFinalValueFee + paypalFee);
		final double profit = sellPrice - totalMoneySpent;
		final double margin = (profit / totalMoneySpent) * 100;
//		System.out.println("Buy price with tax: " + buyPriceWithTax);
//		System.out.println("eBay final value fee: " + eBayFinalValueFee);
//		System.out.println("Paypal fee: " + paypalFee);
//		System.out.println("Total money spent: " + totalMoneySpent);
//		System.out.println("Profit: " + profit);
//		System.out.println("Margin: " + margin + "%");
		return margin;
	}
	
	public static void main(final String[] args) {
		System.out.println(getMarginPercentage(9.98, 14));
	}
	
	/*
	 * Binary search type algorithm because I suck at Math.
	 */
    public static double getPriceFromMargin(final double buyPrice, final double sellShippingCost, final double targetMargin) {
    	final double tolerance = 0.001;
    	double floor = Double.MIN_VALUE;
    	double ceil = Double.MAX_VALUE;
    	double currentMargin = Double.MIN_VALUE;
    	double currentPrice = (floor + ceil) / 2;
    	
    	while(Math.abs(currentMargin - targetMargin) > tolerance) {
    		currentMargin = getMarginPercentage(buyPrice, currentPrice);
    		if(currentMargin > targetMargin) {
    			ceil = currentPrice;
    		} else {
    			floor = currentPrice; 
    		}
    		
    		currentPrice = (floor + ceil) / 2;
    	}
    	
    	return currentPrice - sellShippingCost;
    }
}
