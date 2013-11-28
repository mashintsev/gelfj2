package org.graylog2.log4j2;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HTMLLayout;
import org.graylog2.*;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivan Mashintsev
 */
@Plugin(name = "GELF", category = "Core", elementType = "appender", printObject = true)
public class GelfAppender<T extends Serializable> extends AbstractAppender implements GelfMessageProvider {

	private static String originHost;
	private int graylogPort = 12201;
	private String facility;
	private GelfSender gelfSender;
	private boolean extractStacktrace;
	private boolean addExtendedInformation;
	private boolean includeLocation = true;
	private Map<String, String> fields;

	private GelfAppender(final String name, final Filter filter, final Layout<T> layout, GelfSender gelfSender, final boolean handleExceptions) {
		super(name, filter, layout, handleExceptions);
		this.gelfSender = gelfSender;
	}

	@SuppressWarnings("unchecked")
	public void setAdditionalFields(String additionalFields) {
		fields = (Map<String, String>) JSONValue.parse(additionalFields.replaceAll("'", "\""));
	}

	public String getFacility() {
		return facility;
	}

	public void setFacility(String facility) {
		this.facility = facility;
	}

	public boolean isExtractStacktrace() {
		return extractStacktrace;
	}

	public void setExtractStacktrace(boolean extractStacktrace) {
		this.extractStacktrace = extractStacktrace;
	}

	public String getOriginHost() {
		if (originHost == null) {
			originHost = getLocalHostName();
		}
		return originHost;
	}

	private String getLocalHostName() {
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			error("Unknown local hostname", e);
		}

		return hostName;
	}

	public void setOriginHost(String originHost) {
		GelfAppender.originHost = originHost;
	}

	public boolean isAddExtendedInformation() {
		return addExtendedInformation;
	}

	public void setAddExtendedInformation(boolean addExtendedInformation) {
		this.addExtendedInformation = addExtendedInformation;
	}

	public boolean isIncludeLocation() {
		return this.includeLocation;
	}

	public void setIncludeLocation(boolean includeLocation) {
		this.includeLocation = includeLocation;
	}

	public Map<String, String> getFields() {
		if (fields == null) {
			fields = new HashMap<String, String>();
		}
		return Collections.unmodifiableMap(fields);
	}

	/**
	 * Create a SMTPAppender.
	 *
	 * @param name               The name of the Appender.
	 * @param graylogHost        The Graylog2 host.
	 * @param graylogPortStr     The Graylog2 port.
	 * @param layout             The layout to use.
	 * @param filter             The Filter or null (defaults to ThresholdFilter, level of
	 *                           ERROR).
	 * @param suppressExceptions "true" if exceptions should be hidden from the application,
	 *                           "false" otherwise (defaults to "true").
	 * @return The GelfAppender.
	 */
	@PluginFactory
	public static <S extends Serializable> GelfAppender<S> createAppender(@PluginAttribute("name") final String name,
																		  @PluginAttribute("graylogHost") final String graylogHost,
																		  @PluginAttribute("graylogPort") final String graylogPortStr,
																		  @PluginAttribute("facility") final String facility,
																		  @PluginAttribute("extractStacktrace") final String extractStacktrace,
																		  @PluginAttribute("originHost") final String originHost,
																		  @PluginAttribute("addExtendedInformation") final String addExtendedInformation,
																		  @PluginAttribute("includeLocation") final String includeLocation,
																		  @PluginAttribute("additionalFields") final String additionalFields,
																		  @PluginElement("layout") Layout<S> layout,
																		  @PluginElement("filter") Filter filter,
																		  @PluginAttribute("suppressExceptions") final String suppressExceptions) {
		if (name == null) {
			LOGGER.error("No name provided for GelfAppender");
			return null;
		}

		GelfSender gelfSender = null;
		int graylogPort = -1;
		try {
			graylogPort = Integer.parseInt(graylogPortStr);
		} catch (Exception e) {
			LOGGER.error("Can't parse graylog server port");
		}

		final boolean isHandleExceptions = suppressExceptions == null ? true : Boolean.valueOf(suppressExceptions);

		if (layout == null) {
			@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
			Layout<S> l = (Layout<S>) HTMLLayout.createLayout(null, null, null, null, null, null);
			layout = l;
		}
		if (filter == null) {
			filter = ThresholdFilter.createFilter("INFO", null, null);
		}

		if (graylogHost == null) {
			LOGGER.error("No host provided for GelfAppender");
			return null;
		} else {
			try {
				if (graylogHost.startsWith("tcp:")) {
					String tcpGraylogHost = graylogHost.substring(4);
					gelfSender = getGelfTCPSender(tcpGraylogHost, graylogPort);
				} else if (graylogHost.startsWith("udp:")) {
					String udpGraylogHost = graylogHost.substring(4);
					gelfSender = getGelfUDPSender(udpGraylogHost, graylogPort);
				} else {
					gelfSender = getGelfUDPSender(graylogHost, graylogPort);
				}
			} catch (UnknownHostException e) {
				LOGGER.error("Unknown Graylog2 hostname:" + graylogHost, e);
			} catch (SocketException e) {
				LOGGER.error("Socket exception", e);
			} catch (IOException e) {
				LOGGER.error("IO exception", e);
			} catch (Exception e) {
				return null;
			}
		}
		if (gelfSender != null) {

			GelfAppender gelfAppender = new GelfAppender<S>(name, filter, layout, gelfSender, isHandleExceptions);
			gelfAppender.setFacility(facility);
			gelfAppender.setExtractStacktrace(Boolean.parseBoolean(extractStacktrace));
			gelfAppender.setOriginHost(originHost);
			gelfAppender.setAddExtendedInformation(Boolean.parseBoolean(addExtendedInformation));
			gelfAppender.setIncludeLocation(Boolean.parseBoolean(includeLocation));
			gelfAppender.setAdditionalFields(additionalFields);

			return gelfAppender;
		} else {
			return null;
		}
	}

	protected static GelfUDPSender getGelfUDPSender(String udpGraylogHost, int graylogPort) throws IOException {
		return new GelfUDPSender(udpGraylogHost, graylogPort);
	}

	protected static GelfTCPSender getGelfTCPSender(String tcpGraylogHost, int graylogPort) throws IOException {
		return new GelfTCPSender(tcpGraylogHost, graylogPort);
	}

	@Override
	public void append(LogEvent event) {
		GelfMessage gelfMessage = GelfMessageFactory.makeMessage(event, this);

		if (getGelfSender() == null || !getGelfSender().sendMessage(gelfMessage)) {
			error("Could not send GELF message");
		}
	}

	public GelfSender getGelfSender() {
		return gelfSender;
	}

	public void close() {
		getGelfSender().close();
	}

	public boolean requiresLayout() {
		return false;
	}
}
