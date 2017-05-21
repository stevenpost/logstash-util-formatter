/*
 * Copyright 2017 Karl Spies, Steven Post.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logging.formatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.LogRecord;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 *
 */
public class LogstashUtilFormatter extends ExtFormatter {

    private static final JsonBuilderFactory BUILDER =
            Json.createBuilderFactory(null);
    private static String hostName;
    private final String[] tags = System.getProperty(
            "net.logstash.logging.formatter.LogstashUtilFormatter.tags", "UNKNOWN").split(",");
    private final String[] customfields = System.getProperty(
            "net.logstash.logging.formatter.LogstashUtilFormatter.fields", "").split(",");

    static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host";
        }
    }

    @Override
    public final String format(final ExtLogRecord record) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final String dateString = dateFormat.format(new Date(record.getMillis()));
        final JsonArrayBuilder tagsBuilder = BUILDER.createArrayBuilder();
        for (final String tag : tags) {
            tagsBuilder.add(tag);
        }

        JsonObjectBuilder builder = BUILDER.createObjectBuilder();

        builder.add("@timestamp", dateString)
               .add("level", record.getLevel().toString())
               .add("level_value", record.getLevel().intValue())
               .add("message", formatMessage(record))
               .add("logger_name", record.getLoggerName())
               .add("thread_name", record.getThreadName())
               .add("HOSTNAME", hostName);

        addSourceClassName(record, builder);
        addSourceMethodName(record, builder);
        addThrowableInfo(record, builder);
        addNdc(record, builder);
        for (final String customfield : customfields) {
        	if (!"".equals(customfield)) {
	            final String field[] = customfield.split(":");
	            final String key = field[0];
	            final String value = field[1];
	            builder.add(key, value);
        	}
        }

        builder.add("@tags", tagsBuilder.build());

        addMdc(record, builder);

        return builder.build().toString() + "\n";
    }

    private void addMdc(ExtLogRecord record, JsonObjectBuilder builder) {
    	Map<String, String> mdc = record.getMdcCopy();

    	if (!mdc.isEmpty()) {
	    	JsonObjectBuilder mdcBuilder = BUILDER.createObjectBuilder();

			for (Entry<String, String> entry : mdc.entrySet()) {
				mdcBuilder.add(entry.getKey(), entry.getValue());
			}

			builder.add("@mdc", mdcBuilder.build());
    	}
	}

	@Override
    public synchronized String formatMessage(final LogRecord record) {
        String message = super.formatMessage(record);

        try {
            final Object parameters[] = record.getParameters();
            if (message == record.getMessage() && parameters != null && parameters.length > 0) {
                message = String.format(message, parameters);
            }
        } catch (Exception ex) {
        }

        return message;
    }

	private void addNdc(final ExtLogRecord record, JsonObjectBuilder builder) {
		if (record.getNdc() != null && !"".equals(record.getNdc())) {
			builder.add("ndc", record.getNdc());
		}
	}

    /**
     * Format the stacktrace.
     *
     * @param record the logrecord which contains the stacktrace
     * @param builder the json object builder to append
     */
    final void addThrowableInfo(final LogRecord record, final JsonObjectBuilder builder) {
        if (record.getThrown() != null) {
        	builder.add("line_number", getLineNumber(record));
            if (record.getSourceClassName() != null) {
                builder.add("exception_class",
                        record.getThrown().getClass().getName());
            }
            if (record.getThrown().getMessage() != null) {
                builder.add("exception_message",
                        record.getThrown().getMessage());
            }
            addStacktraceElements(record, builder);
        }
    }

    /**
     * Get the line number of the exception.
     *
     * @param record the logrecord
     * @return the line number
     */
    final int getLineNumber(final LogRecord record) {
        final int lineNumber;
        if (record.getThrown() != null) {
            lineNumber = getLineNumberFromStackTrace(
                    record.getThrown().getStackTrace());
        } else {
            lineNumber = 0;
        }
        return lineNumber;
    }

    /**
     * Gets line number from stack trace.
     * @param traces all stack trace elements
     * @return line number of the first stacktrace.
     */
    final int getLineNumberFromStackTrace(final StackTraceElement[] traces) {
        final int lineNumber;
        if (traces.length > 0 && traces[0] != null) {
            lineNumber = traces[0].getLineNumber();
        } else {
            lineNumber = 0;
        }
        return lineNumber;
    }

    final void addValue(final JsonObjectBuilder builder, final String key, final String value) {
        if (value != null) {
            builder.add(key, value);
        } else {
            builder.add(key, "null");
        }
    }

    private void addSourceMethodName(final LogRecord record, final JsonObjectBuilder builder) {
        addValue(builder, "method", record.getSourceMethodName());
    }

    private void addSourceClassName(final LogRecord record, final JsonObjectBuilder builder) {
        addValue(builder, "class", record.getSourceClassName());
    }

    private void addStacktraceElements(final LogRecord record, final JsonObjectBuilder builder) {
        final StringWriter sw = new StringWriter();
        record.getThrown().printStackTrace(new PrintWriter(sw));
        builder.add("stack_trace", sw.toString());
    }
}
