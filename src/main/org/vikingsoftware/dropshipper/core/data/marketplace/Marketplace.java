package main.org.vikingsoftware.dropshipper.core.data.marketplace;

import java.net.MalformedURLException;
import java.net.URL;

public class Marketplace {
	
	public final int id;
	public final String marketplace_name;
	public final URL marketplace_url;
	public final double marketplace_profit_cut;
	
	private Marketplace(final Builder builder) {
		this.id = builder.id;
		this.marketplace_name = builder.marketplace_name;
		this.marketplace_url = builder.marketplace_url;
		this.marketplace_profit_cut = builder.marketplace_profit_cut;
	}
	
	public static class Builder {
		private int id;
		private String marketplace_name;
		private URL marketplace_url;
		private double marketplace_profit_cut;
		
		public Builder id(final int id) {
			this.id = id;
			return this;
		}
		
		public Builder marketplace_name(final String name) {
			this.marketplace_name = name;
			return this;
		}
		
		public Builder marketplace_url(final String url) {
			try {
				this.marketplace_url = new URL(url);
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			}
			return this;
		}
		
		public Builder marketplace_profit_cut(final double cut) {
			this.marketplace_profit_cut = cut;
			return this;
		}
		
		public Marketplace build() {
			return new Marketplace(this);
		}
	}
}
