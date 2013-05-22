GELFJ - A GELF Appender for Log4j2
====================================================================

### WARNING: Latest version of this library is not compatible with graylog2-server < 0.9.6. 

What is GELFJ
-------------

It's very simple GELF implementation in pure Java with the Log4j2 appender. It supports chunked messages which allows you to send large log messages (stacktraces, environment variables, additional fields, etc.) to a [Graylog2](http://www.graylog2.org/) server.

How to use GELFJ
----------------

Drop the latest JAR into your classpath and configure Log4j2 to use it.

Log4j2 appender
--------------

GelfAppender will use the log message as a short message and a stacktrace (if exception available) as a long message if "extractStacktrace" is true.

To use GELF Facility as appender in Log4j2 (XML configuration format):

	<GELF
			name="graylog2"
			graylogHost="127.0.0.1"
			graylogPort="12201"
			originHost="my.machine.example.com"
			additionalFields="{'environment': 'DEV', 'application': 'MyAPP'}"
			extractStacktrace="true"
			addExtendedInformation="true"
			facility="USER"
			includeLocation="true">
		<PatternLayout>
			<pattern>%d %-5p [%t] %C{2} (%F:%L) - %m%n</pattern>
		</PatternLayout>
	</GELF>

and then add it as a one of appenders:

    <root>
        <priority value="INFO"/>
        <appender-ref ref="graylog2"/>
    </root>

Options
-------

GelfAppender supports the following options:

- **graylogHost**: Graylog2 server where it will send the GELF messages
- **graylogPort**: Port on which the Graylog2 server is listening; default 12201 (*optional*)
- **originHost**: Name of the originating host; defaults to the local hostname (*optional*)
- **extractStacktrace** (true/false): Add stacktraces to the GELF message; default false (*optional*)
- **addExtendedInformation** (true/false): Add extended information like Log4j's NDC/MDC; default false (*optional*)
- **includeLocation** (true/false): Include caller file name and line number. Log4j documentation warns that generating caller location information is extremely slow and should be avoided unless execution speed is not an issue; default true (*optional*)
- **facility**: Facility which to use in the GELF message; default "gelf-java"

What is GELF
------------

The Graylog Extended Log Format (GELF) avoids the shortcomings of classic plain syslog:

- Limited to length of 1024 byte
- Not much space for payloads like stacktraces
- Unstructured. You can only build a long message string and define priority, severity etc.

You can get more information here: [http://www.graylog2.org/about/gelf](http://www.graylog2.org/about/gelf)