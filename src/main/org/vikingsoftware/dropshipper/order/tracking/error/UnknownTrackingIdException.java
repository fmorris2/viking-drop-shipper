package main.org.vikingsoftware.dropshipper.order.tracking.error;

public class UnknownTrackingIdException extends RuntimeException {

	private static final long serialVersionUID = -7209464577898582647L;

	public UnknownTrackingIdException(final String msg) {
		super(msg);
	}

}
