package main.org.vikingsoftware.dropshipper.core.utils;

public final class PriceUtils {
	
	private static final double EBAY_FEES = .13;
	private static final double SALES_TAX = .07;
	
	private PriceUtils() {}

	public static double getMarginPercentage(final double buyPrice, final double sellPrice) {
		final double buyPriceWithTax = buyPrice * (1D + SALES_TAX);
		final double sellPriceWithFees = sellPrice * (1D - EBAY_FEES);
		final double profit = sellPriceWithFees - buyPriceWithTax;
		final double margin = (profit / (buyPriceWithTax + (sellPrice * EBAY_FEES))) * 100;
		return margin;
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
    	
    	return currentPrice;
    }
}
