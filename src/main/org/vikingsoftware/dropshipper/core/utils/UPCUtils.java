package main.org.vikingsoftware.dropshipper.core.utils;

public final class UPCUtils {
	
	private UPCUtils() {
		//utils class
	}
	
	public static int getCheckDigit(long num) {
		int oddSum = 0, evenSum = 0, bigSum;
		boolean odd = true;

		while (num != 0) {
			if (odd) {
				oddSum += num % 10;
				odd = false;
			} else {
				evenSum += num % 10;
				odd = true;
			}
			num = (num - num % 10) / 10; // reduce number
		}

		oddSum *= 3; // sum of odd number multiplied by 3
		bigSum = oddSum + evenSum; // add both numbers together
		final int remainder = bigSum % 10;
		return remainder == 0 ? 0 : 10 - remainder; // return the sum of the numbers % 10
	}
}
