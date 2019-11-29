package main.org.vikingsoftware.dropshipper.order.tracking.handler.impl.sams;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;

import main.org.vikingsoftware.dropshipper.core.google.GmailApi;

public final class SamsClubTrackingEmailRepository {
	
	private static final long REFRESH_CYCLE_TIME_MS = 60_000 * 60; //every 60 minutes
	private static final String SHIPPING_EMAILS_LABEL_ID = "Label_724940738117254674";
	
	private static final Pattern ORDER_NUM_PATTERN = Pattern.compile("Order # (\\d+)");
	private static final Pattern TRACKING_NUM_PATTERN = Pattern.compile("\\?trackingId=([\\d\\w]+)\\\"");
	
	private static SamsClubTrackingEmailRepository instance;
	
	private Set<String> parsedEmails = new HashSet<>(); //contains message ids
	private Map<String, String> trackingDetails = new HashMap<>(); //maps transaction id --> tracking num
	private long lastRefreshMs;
	
	private SamsClubTrackingEmailRepository() {
		//singleton
	}
	
	public static void main(final String[] args) {
		SamsClubTrackingEmailRepository.get();
	}
	
	public static synchronized SamsClubTrackingEmailRepository get() {
		if(instance == null) {
			instance = new SamsClubTrackingEmailRepository();
		}
		
		if(instance.needsRefresh()) {
			instance.refresh();
		}
		
		return instance;
	}
	
	private boolean needsRefresh() {
		return System.currentTimeMillis() - lastRefreshMs >= REFRESH_CYCLE_TIME_MS;
	}
	
	private void refresh() {
		final GmailApi gmailApi = GmailApi.get();
		final List<Message> messages = gmailApi.getEmailsFromLabels(Collections.singletonList(SHIPPING_EMAILS_LABEL_ID));
		for(final Message message : messages) {
			if(parsedEmails.contains(message.getId())) {
				continue;
			}
			
			final Message details = gmailApi.getSpecificEmailDetails(message.getId());
			if(details != null) {
				parseEmail(details);
			}
		}
	}
	
	private void parseEmail(final Message email) {
		final MessagePart payload = email == null ? null : email.getPayload();
		if(payload != null) {
			final MessagePartBody body = payload.getBody();
			final String base64Body = body.getData();
			final byte[] decoded = Base64.decodeBase64(base64Body);
			try {
				final String rawBody = new String(decoded, "UTF-8");
				final Matcher orderNumMatcher = ORDER_NUM_PATTERN.matcher(rawBody);
				final Matcher trackingNumMatcher = TRACKING_NUM_PATTERN.matcher(rawBody);
				if(orderNumMatcher.find() && trackingNumMatcher.find()) {
					System.out.println("Successfully parsed Sams Tracking Email: " + orderNumMatcher.group(1) + " --> " + trackingNumMatcher.group(1));
					trackingDetails.put(orderNumMatcher.group(1), trackingNumMatcher.group(1));
				} else {
					System.out.println("Could not parse tracking info from email w/ id " + email.getId());
				}
				
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getTrackingNumberForTransactionId(final String transactionId) {
		return trackingDetails.get(transactionId);
	}
}
