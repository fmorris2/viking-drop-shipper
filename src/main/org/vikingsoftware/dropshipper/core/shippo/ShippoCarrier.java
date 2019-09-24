package main.org.vikingsoftware.dropshipper.core.shippo;

public enum ShippoCarrier {
	APC_POSTAL("apc_postal"),
	AUSTRALIA_POST("australia_post"),
	ARAMEX("aramex"),
	ASENDIA("asendia_us"),
	AXLE_HIRE("axlehire"),
	BORDER_GURU("borderguru"),
	BOXBERRY("boxberry"),
	BRING("bring"),
	CANADA_POST("canada_post"),
	CDL("cdl"),
	CORREIOS_BRAZIL("correios_br"),
	CORREOS_ESPANA("correos_espana"),
	COLLECT_PLUS("collect_plus"),
	COURIERS_PLEASE("couriersplease"),
	DEUTSCHE_POST("deutsche_post"),
	DHL_BENELUX("dhl_beleux"),
	DHL_GERMANY("dhl_germany"),
	DHL_ECOMMERCE("dhl_ecommerce"),
	DHL_EXPRESS("dhl_express"),
	DPD_GERMANY("dpd_germany"),
	DPD_UK("dpd_uk"),
	ESTAFETA("estafeta"),
	FASTWAY_AUSTRALIA("fastway_australia"),
	FEDEX("fedex"),
	GLS_GERMANY("gls_de"),
	GLD_FRANCE("gls_fr"),
	GLOBEGISTICS("globegistics"),
	GOPHR("gophr"),
	GSO("gso"),
	HERMES_UK("hermes_uk"),
	HONG_KONG_POST("hongkong_post"),
	LASERSHIP("lasership"),
	MONDIAL_RELAY("mondial_relay"),
	NEW_ZEALAND_POST("new_zealand_post"),
	NEWGISTICS("newgistics"),
	NIPPON_EXPRESS("nippon_express"),
	ONTRAC("ontrac"),
	ORANGE_DS("orangeds"),
	PARCEL("parcel"),
	POSTI("posti"),
	PUROLATOR("purolator"),
	RR_DONNELLEY("rr_donnelley"),
	RUSSIAN_POST("russian_post"),
	SENDLE("sendle"),
	SKY_POSTAL("skypostal"),
	STUART("stuart"),
	UPS("ups"),
	USPS("usps"),
	YODEL("yodel");
	
	public final String apiToken;
	
	ShippoCarrier(final String apiToken) {
		this.apiToken = apiToken;
	}
	
	/*
	 * Used for mapping MysteryTracking identifiers to the appropriate ShippoCarrier enum entry
	 */
	public static ShippoCarrier getCarrier(final String identifier) {
		switch(identifier.toLowerCase()) {
		
			case "dhl":
				return DHL_EXPRESS;
			case "fedex":
				return FEDEX;
			case "ontrac":
				return ONTRAC;
			case "ups":
				return UPS;
			case "usps":
				return USPS;
					
		}
		return null;
	}
}
