package main.org.vikingsoftware.dropshipper.order.executor.error;

public class OrderExecutionException extends RuntimeException {

	private static final long serialVersionUID = 7843649002151495003L;

	public OrderExecutionException(final String msg) {
		super(msg);
	}
}
