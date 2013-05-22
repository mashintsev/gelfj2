package org.graylog2.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.graylog2.GelfMessage;
import org.graylog2.GelfMessageProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class GelfMessageFactory {

	public static final int MAX_SHORT_MESSAGE_LENGTH = 250;
	public static final String ORIGIN_HOST_KEY = "originHost";
	public static final String LOGGER_NAME = "logger";
	public static final String LOGGER_NDC = "loggerNdc";
	public static final String THREAD_NAME = "thread";
	public static final String JAVA_TIMESTAMP = "timestampMs";

	@SuppressWarnings("unchecked")
	public static GelfMessage makeMessage(LogEvent event, GelfMessageProvider provider) {
		long timeStamp = Log4jVersionChecker.getTimeStamp(event);
		Level level = event.getLevel();

		String file = null;
		String lineNumber = null;
		if (provider.isIncludeLocation()) {
			file = event.getFQCN();
			lineNumber = String.valueOf(event.getSource().getLineNumber());
		}

		String renderedMessage = event.getMessage().getFormattedMessage();
		String shortMessage;

		if (renderedMessage == null) {
			renderedMessage = "";
		}

		if (provider.isExtractStacktrace()) {
			Throwable throwable = event.getThrown();
			if (throwable != null) {
				renderedMessage += "\n\r" + extractStacktrace(throwable);
			}
		}

		if (renderedMessage.length() > MAX_SHORT_MESSAGE_LENGTH) {
			shortMessage = renderedMessage.substring(0, MAX_SHORT_MESSAGE_LENGTH - 1);
		} else {
			shortMessage = renderedMessage;
		}

		GelfMessage gelfMessage = new GelfMessage(shortMessage, renderedMessage, timeStamp,
				String.valueOf(getSyslogEquivalent(level)), lineNumber, file);

		if (provider.getOriginHost() != null) {
			gelfMessage.setHost(provider.getOriginHost());
		}

		if (provider.getFacility() != null) {
			gelfMessage.setFacility(provider.getFacility());
		}

		Map<String, String> fields = provider.getFields();
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			if (entry.getKey().equals(ORIGIN_HOST_KEY) && gelfMessage.getHost() == null) {
				gelfMessage.setHost(fields.get(ORIGIN_HOST_KEY));
			} else {
				gelfMessage.addField(entry.getKey(), entry.getValue());
			}
		}

		if (provider.isAddExtendedInformation()) {

			gelfMessage.addField(THREAD_NAME, event.getThreadName());
			gelfMessage.addField(LOGGER_NAME, event.getLoggerName());
			gelfMessage.addField(JAVA_TIMESTAMP, Long.toString(gelfMessage.getJavaTimestamp()));

			// Get MDC data
			Map<String, String> mdc = event.getContextMap();

			if (mdc != null) {
				for (Map.Entry<String, String> entry : mdc.entrySet()) {
					gelfMessage.addField(entry.getKey(), entry.getValue());
				}
			}

			// Get NDC and add a GELF field
			String ndc = event.getContextStack().peek();

			if (ndc != null) {
				gelfMessage.addField(LOGGER_NDC, ndc);
			}
		}

		return gelfMessage;
	}

	private static int getSyslogEquivalent(Level level) {
		switch (level) {
			case OFF:
				return 0;
			case FATAL:
				return 0;
			case ERROR:
				return 3;
			case WARN:
				return 4;
			case INFO:
				return 6;
			case DEBUG:
				return 7;
			case TRACE:
				return 7;
			default:
				return 7;
		}
	}

	private static String extractStacktrace(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}
}
