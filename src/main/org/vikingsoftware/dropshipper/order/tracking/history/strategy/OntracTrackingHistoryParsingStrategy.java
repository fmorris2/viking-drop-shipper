package main.org.vikingsoftware.dropshipper.order.tracking.history.strategy;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.org.vikingsoftware.dropshipper.core.data.processed.order.ProcessedOrder;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingHistoryRecord;
import main.org.vikingsoftware.dropshipper.core.tracking.TrackingStatus;
import main.org.vikingsoftware.dropshipper.order.tracking.history.TrackingHistoryParsingStrategy;

public class OntracTrackingHistoryParsingStrategy implements TrackingHistoryParsingStrategy {
	
	private static final String INITIAL_URL_TEMPLATE = "https://www.ontrac.com/trackingres.asp?tracking_number=";
	private static final Pattern DETAILS_LINK_REGEX = Pattern.compile("trackingdetail\\.asp\\?tracking=");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d uuuu h:ma");
	
	@Override
	public List<TrackingHistoryRecord> parse(final ProcessedOrder order) {
		final String trackingNumber = order.tracking_number;
		System.out.println("OntracTrackingHistoryParsingStrategy#parse("+trackingNumber+")");
		
		try {
			final Document document = Jsoup.connect(INITIAL_URL_TEMPLATE + trackingNumber).get();
			final Elements detailsLinkEl = document.getElementsByAttributeValueMatching("href", DETAILS_LINK_REGEX);
			if(!detailsLinkEl.isEmpty()) {
				final String detailsHref = detailsLinkEl.first().attr("href");
				final Document detailsPage = Jsoup.connect("https://www.ontrac.com/" + detailsHref).get();
				final Element detailsElement = detailsPage.getElementById("trkdetail");
				if(detailsElement != null) {
					final Elements detailsTable = detailsElement.getElementsByTag("table");
					if(!detailsTable.isEmpty()) {
						return parseTrackingHistoryRecordFromDetailsTable(order, detailsTable.first());
					}
				}
				
			}
			
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private List<TrackingHistoryRecord> parseTrackingHistoryRecordFromDetailsTable(final ProcessedOrder order, final Element detailsTable) {
		
		System.out.println("OntracTrackingHistoryParsingStrategy#parseTrackingHistoryRecordFromDetailsTable");
		final List<TrackingHistoryRecord> updates = new ArrayList<>();
		final Element embeddedTable = detailsTable.getElementsByTag("table").first();
		if(embeddedTable != null) {
			final Elements rows = embeddedTable.getElementsByTag("tr");
			Map<Integer, String> colIdxToHeaderMappings = new HashMap<>();
			for(final Element row : rows) {
				final Elements headers = row.getElementsByTag("th");
				if(!headers.isEmpty()) {
					for(int headerIdx = 0; headerIdx < headers.size(); headerIdx++) {
						colIdxToHeaderMappings.put(headerIdx, headers.get(headerIdx).text());
					}
					System.out.println("Parsed header indexes: " + headers);
				} else { //non-header row
					final DetailsTableRow tableRow = new DetailsTableRow();
					final Elements cols = row.getElementsByTag("td");
					for(int colIdx = 0; colIdx < cols.size(); colIdx++) {
						tableRow.cells.put(colIdxToHeaderMappings.get(colIdx), cols.get(colIdx).text());
					}
					System.out.println("Parsed table row cells: " + tableRow.cells);
					updates.add(tableRow.convertToTrackingHistoryRecord(order));
				}
			}
		}
		
		return updates.size() == order.currentNumTrackingHistoryEvents ? Collections.emptyList() : updates;
	}
	
	private final class DetailsTableRow {
		
		private static final String tracking_status_details_col = "Transaction";
		private static final String tracking_status_date_col = "Date / Time";
		private static final String tracking_location_city_col = "City";
		
		public Map<String, String> cells = new HashMap<>();
		
		public TrackingHistoryRecord convertToTrackingHistoryRecord(final ProcessedOrder order) {
			final String details = cells.get(tracking_status_details_col);
			final String date = cells.get(tracking_status_date_col);
			final String city = cells.get(tracking_location_city_col);
			
			if(details == null || date == null || city == null) {
				return null;
			}
			
			final LocalDateTime localDateTime = LocalDateTime.parse(date, DATE_FORMAT);
			final TrackingStatus trackingStatus = convertToTrackingStatus(details);
			
			System.out.println("Building tracking history record from parsed Ontrac details");
			return new TrackingHistoryRecord.Builder()
					.processed_order_id(order.id)
					.tracking_status(trackingStatus)
					.tracking_status_date(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli())
					.tracking_status_details(details)
					.tracking_location_city(city)
					.build();			 
		}
		
		private TrackingStatus convertToTrackingStatus(final String details) {
			switch(details.toLowerCase()) {
			   case "data entry":
			      return TrackingStatus.PRE_TRANSIT;
			   case "package received at facility":
			   case "out for delivery":
				  return TrackingStatus.TRANSIT;
			   case "delivered":
				   return TrackingStatus.DELIVERED;
			   default:
				   return TrackingStatus.UNKNOWN;
			}
		}
		
	}

}
