package main.org.vikingsoftware.dropshipper.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import main.org.vikingsoftware.dropshipper.core.db.impl.VDSDBManager;

public class DBLogging {

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final Queue<LogMessage> messageQueue = new ConcurrentLinkedQueue<>();

	static {
		executor.scheduleAtFixedRate(() -> updateLogs(), 0, 5000, TimeUnit.MILLISECONDS);
	}

	private static void updateLogs() {
		try {
			final PreparedStatement st = VDSDBManager.get().createPreparedStatement("INSERT INTO logging(class,level,message,exception)"
					+ " VALUES(?,?,?,?)");
			while(!messageQueue.isEmpty()) {
				final LogMessage msg = messageQueue.poll();
				st.setString(1, msg.clazz.getName());
				st.setString(2, msg.level.name());
				st.setString(3, msg.message);
				st.setString(4, convertExceptionToString(msg.exception));
				st.addBatch();
			}

			st.executeBatch();
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

	public static void low(final Class<?> clazz, final String msg, final Exception exception) {
		if(exception != null) {
			exception.printStackTrace();
		}
		messageQueue.add(new LogMessage(clazz, LogLevel.LOW, exception, msg));
	}

	public static void medium(final Class<?> clazz, final String msg, final Exception exception) {
		if(exception != null) {
			exception.printStackTrace();
		}
		messageQueue.add(new LogMessage(clazz, LogLevel.MEDIUM, exception, msg));
	}

	public static void high(final Class<?> clazz, final String msg, final Exception exception) {
		if(exception != null) {
			exception.printStackTrace();
		}
		messageQueue.add(new LogMessage(clazz, LogLevel.HIGH, exception, msg));
	}

	public static void critical(final Class<?> clazz, final String msg, final Exception exception) {
		if(exception != null) {
			exception.printStackTrace();
		}
		messageQueue.add(new LogMessage(clazz, LogLevel.CRITICAL, exception, msg));
	}

	private static String convertExceptionToString(final Exception e) {
		if(e == null) {
			return null;
		}
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}

	public static enum LogLevel {
		LOW,
		MEDIUM,
		HIGH,
		CRITICAL
	}
	private static class LogMessage {
		private final Class<?> clazz;
		private final LogLevel level;
		private final Exception exception;
		private final String message;

		public LogMessage(final Class<?> clazz, final LogLevel level, final Exception exception, final String msg) {
			this.clazz = clazz;
			this.level = level;
			this.exception = exception;
			this.message = msg;
		}
	}
}
