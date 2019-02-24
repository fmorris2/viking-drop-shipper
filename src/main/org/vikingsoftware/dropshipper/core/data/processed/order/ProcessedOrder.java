package main.org.vikingsoftware.dropshipper.core.data.processed.order;

public class ProcessedOrder {
	
	public final int customer_order_id;
	public final int fulfillment_listing_id;
	public final String fulfillment_transaction_id;
	public final String order_status;
	
	public ProcessedOrder(final int cid, final int fid, final String ftid, final String status) {
		this.customer_order_id = cid;
		this.fulfillment_listing_id = fid;
		this.fulfillment_transaction_id = ftid;
		this.order_status = status;
	}
}
