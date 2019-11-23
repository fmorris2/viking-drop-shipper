package main.org.vikingsoftware.dropshipper.core.tracking;

public final class TrackingHistoryRecord {
	
	public final int processed_order_id;
	public final TrackingStatus tracking_status;
	public final long tracking_status_date;
	public final String tracking_status_details;
	public final String tracking_location_city;
	public final String tracking_location_state;
	public final String tracking_location_zip;
	public final String tracking_location_country;
	
	private TrackingHistoryRecord(final Builder builder) {
		this.processed_order_id = builder.processed_order_id;
		this.tracking_status = builder.tracking_status;
		this.tracking_status_date = builder.tracking_status_date;
		this.tracking_status_details = builder.tracking_status_details;
		this.tracking_location_city = builder.tracking_location_city;
		this.tracking_location_country = builder.tracking_location_country;
		this.tracking_location_state = builder.tracking_location_state;
		this.tracking_location_zip = builder.tracking_location_zip;
	}
	
	public static final class Builder {
		
		private int processed_order_id;
		private TrackingStatus tracking_status;
		private long tracking_status_date;
		private String tracking_status_details;
		private String tracking_location_city;
		private String tracking_location_state;
		private String tracking_location_zip;
		private String tracking_location_country;
		
		public Builder processed_order_id(final int id) {
			this.processed_order_id = id;
			return this;
		}
		
		public Builder tracking_status(final TrackingStatus status) {
			this.tracking_status = status;
			return this;
		}
		
		public Builder tracking_status_date(final long date) {
			this.tracking_status_date = date;
			return this;
		}
		
		public Builder tracking_status_details(final String details) {
			this.tracking_status_details = details;
			return this;
		}
		
		public Builder tracking_location_city(final String city) {
			this.tracking_location_city = city;
			return this;
		}
		
		public Builder tracking_location_state(final String state) {
			this.tracking_location_state = state;
			return this;
		}
		
		public Builder tracking_location_zip(final String zip) {
			this.tracking_location_zip = zip;
			return this;
		}
		
		public Builder tracking_location_country(final String country) {
			this.tracking_location_country = country;
			return this;
		}
		
		public TrackingHistoryRecord build() {
			return new TrackingHistoryRecord(this);
		}
	}
}
