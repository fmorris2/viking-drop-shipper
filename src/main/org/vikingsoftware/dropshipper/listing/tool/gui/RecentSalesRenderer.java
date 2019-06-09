package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JEditorPane;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class RecentSalesRenderer extends JEditorPane {

	private static final long serialVersionUID = 1L;
	private static final String IPHONE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15F79 Instagram 52.0.0.14.164 (iPhone8,2; iOS 11_4; pt_BR; pt-BR; scale=2.61; gamut=normal; 1080x1920)";
	
	public RecentSalesRenderer() {
		super.setEditable(false);
		super.setContentType("text/html");
		super.setMaximumSize(new Dimension(300, 800));
	}
	
	public void renderUrl(final String url) {
		try {
			final Document doc = Jsoup
					.connect(url)
					.userAgent(IPHONE_USER_AGENT)
					.get();
			super.setText(doc.html());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
