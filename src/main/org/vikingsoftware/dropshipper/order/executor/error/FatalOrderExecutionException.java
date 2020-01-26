package main.org.vikingsoftware.dropshipper.order.executor.error;

public class FatalOrderExecutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public FatalOrderExecutionException(final String str) {
		super(str);
	}

}
