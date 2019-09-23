package main.org.vikingsoftware.dropshipper.order.tracking.shippo.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import spark.Spark;

public final class ShippoWebhookServer {
	
	private static final int SERVER_PORT = 4567;
	
	private static ShippoWebhookServer instance;
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private ShippoWebhookServer() {
		
	}
	
	public static synchronized ShippoWebhookServer get() {
		if(instance == null) {
			instance = new ShippoWebhookServer();
		}
		
		return instance;
	}
	
	public static void main(final String[] args) {
		ShippoWebhookServer.get().start();
	}
	
	public void start() {
		executor.execute(() -> {
			
			Spark.port(SERVER_PORT);
			
			Spark.get("/test", (req, res) -> "TEST");
			
			Spark.post("/shippo/track-updated", (req, res) -> {
				System.out.println("Request received from shippo!");
				return "Received";
			});
		});
	}
}
