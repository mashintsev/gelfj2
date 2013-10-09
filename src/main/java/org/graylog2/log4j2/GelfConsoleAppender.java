package org.graylog2.log4j2;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HTMLLayout;
import org.graylog2.*;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jay Faulkner
 */
@Plugin(name = "GELF", category = "Core", elementType = "appender", printObject = true)
public class GelfConsoleAppender<T extends Serializable> extends AbstractAppender<T> implements GelfMessageProvider {

    private static String originHost;
    private String facility;
    private boolean extractStacktrace;
    private boolean addExtendedInformation;
    private boolean includeLocation = true;
    private Map<String, String> fields;

    private GelfConsoleAppender(final String name, final Filter filter, final Layout<T> layout, GelfSender gelfSender, final boolean handleExceptions) {
        super(name, filter, layout, handleExceptions);
        this.gelfSender = gelfSender;
    }

    public enum Target {
        /** Standard output. */
        SYSTEM_OUT,
        /** Standard error */
        SYSTEM_ERR
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
        GelfConsoleAppender.originHost = originHost;
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

    @PluginFactory
    public static <S extends Serializable> GelfConsoleAppender<S> createAppender(@PluginAttr("name") final String name,
                                                                          @PluginAttr("facility") final String facility,
                                                                          @PluginAttr("target") final String t,
                                                                          @PluginAttr("extractStacktrace") final String extractStacktrace,
                                                                          @PluginAttr("originHost") final String originHost,
                                                                          @PluginAttr("addExtendedInformation") final String addExtendedInformation,
                                                                          @PluginAttr("includeLocation") final String includeLocation,
                                                                          @PluginAttr("additionalFields") final String additionalFields,
                                                                          @PluginElement("layout") Layout<S> layout,
                                                                          @PluginElement("filter") Filter filter,
                                                                          @PluginAttr("suppressExceptions") final String suppressExceptions) {
        if (name == null) {
            LOGGER.error("No name provided for GelfConsoleAppender");
            return null;
        }

        GelfSender gelfSender = null;

        final boolean isHandleExceptions = suppressExceptions == null ? true : Boolean.valueOf(suppressExceptions);

        if (layout == null) {
            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
            Layout<S> l = (Layout<S>) HTMLLayout.createLayout(null, null, null, null, null, null);
            layout = l;
        }
        if (filter == null) {
            filter = ThresholdFilter.createFilter("INFO", null, null);
        }

        // If target is set properly, use it. Otherwise use SYSTEM_OUT
        final Target target = t == null ? Target.SYSTEM_OUT : Target.valueOf(t)

        gelfSender = getGelfConsoleSender(target);

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

    protected static GelfConsoleSender getGelfConsoleSender(Target target) throws IOException {
        return new GelfConsoleSender(target);
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
