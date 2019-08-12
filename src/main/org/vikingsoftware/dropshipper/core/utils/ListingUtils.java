package main.org.vikingsoftware.dropshipper.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import main.org.vikingsoftware.dropshipper.listing.tool.logic.Listing;

public final class ListingUtils {
	
	private ListingUtils() {
		//utility class shouldn't be instantiated
	}
	
	public static void makeDescriptionPretty(final Listing listing) {
		final Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		tidy.setIndentContent(true);
		tidy.setPrintBodyOnly(true);
		tidy.setTidyMark(false);

		final Document htmlDom = tidy.parseDOM(new ByteArrayInputStream(listing.description.getBytes()), null);
		final OutputStream out = new ByteArrayOutputStream();
		tidy.pprint(htmlDom, out);

		String replacedDesc = out.toString();
		replacedDesc = replacedDesc.replaceAll("<!DOCTYPE[^>]*(>?)", "");
		replacedDesc = replacedDesc.replaceAll("<\\/*html.*>", "");
		replacedDesc = replacedDesc.replaceAll("<head>(?:.|\\n|\\r)+?<\\/head>", "");
		replacedDesc = replacedDesc.replaceAll("<\\/*body>", "");
		replacedDesc = replacedDesc.replaceAll("(\\\"\\s*|;\\s*)(max-)*width\\:\\s*([1-9]\\d{3,}|9[3-9]\\d|9[4-9]{2})(px|%)(\\s*!important)*", "\1\2width:923px!important");
		replacedDesc = replacedDesc.replaceAll("width=\"([1-9]\\d{3,}|9[3-9]\\d|9[4-9]{2})\"", "width=\"923\"");
		replacedDesc = replacedDesc.replaceAll("height=\"\\d+\"", "");

		listing.description =
				"<div style=\"font-family: Helvetica; max-width: 923px\">" +
						replacedDesc +
				"</div>";
	}
}
