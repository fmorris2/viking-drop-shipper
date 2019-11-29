package main.org.vikingsoftware.dropshipper.core.google;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

public final class GmailApi {
	
	private static final String CREDS_FILE = "/data/google-api-creds.p12";
	private static final String EMAIL_ADDRESS = "sales@vikingwholesale.org";
	
	private static GmailApi instance;
	
	private final List<String> scopes = Collections.singletonList(GmailScopes.GMAIL_READONLY);
	private final JsonFactory jsonFactory;
	private final Gmail service;
	
	public static void main(final String[] args) {
		GmailApi.get();
	}
	
	private GmailApi() throws IOException {
		jsonFactory = JacksonFactory.getDefaultInstance();
		service = generateGmailService();
	}
	
	public static synchronized GmailApi get() {
		if(instance == null) {
			try {
				instance = new GmailApi();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		
		return instance;
	}
	
	public List<Message> getEmailsFromLabels(final List<String> labels) {
		List<Message> emails = new ArrayList<>();
		try {
			final ListMessagesResponse res = service.users()
					.messages()
					.list(EMAIL_ADDRESS)
					.setLabelIds(labels)
					.setMaxResults(1000L)
					.execute();
			emails.addAll(res.getMessages());
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return emails;
	}
	
	public Message getSpecificEmailDetails(final String id) {
		Message email = null;
		try {
			email = service.users()
				.messages()
				.get(EMAIL_ADDRESS, id)
				.execute();
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return email;
	}
	
	private Gmail generateGmailService() {
		try {
			final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			final Credential credential = generateCredentials(httpTransport);
			final Gmail service = new Gmail.Builder(httpTransport, jsonFactory, credential)
					.setApplicationName("Viking Drop Shipper")
					.build();
			return service;
		} catch(final Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private Credential generateCredentials(final NetHttpTransport httpTransport) throws Exception {
        final URL resource = GmailApi.class.getResource(CREDS_FILE);
        final File file = new File(resource.toURI());
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("Resource not found: " + CREDS_FILE);
        }
        
        final GoogleCredential credential = new GoogleCredential.Builder()
        	    .setTransport(httpTransport)
        	    .setJsonFactory(jsonFactory)
        	    .setServiceAccountId("vsds-906@vsds-1574982874087.iam.gserviceaccount.com")
        	    .setServiceAccountPrivateKeyFromP12File(file)
        	    .setServiceAccountScopes(scopes)
        	    .setServiceAccountUser(EMAIL_ADDRESS)
        	    .build();
        
        credential.refreshToken();
        return credential;              
	}
}
