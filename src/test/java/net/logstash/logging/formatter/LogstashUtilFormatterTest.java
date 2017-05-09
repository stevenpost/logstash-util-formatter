/*
 * Copyright 2013 karl spies.
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class LogstashUtilFormatterTest {

    private static final String EXPECTED_EX_STACKTRACE = "java.lang.Exception: That is an exception\n"
            + "\tat Test.methodTest(Test.class:42)\n"
            + "Caused by: java.lang.Exception: This is the cause\n"
            + "\tat Cause.methodCause(Cause.class:69)\n";

    private Exception cause = buildException("This is the cause", null,
            new StackTraceElement("Cause", "methodCause", "Cause.class", 69));

    private Exception ex = buildException("That is an exception", cause,
            new StackTraceElement("Test", "methodTest", "Test.class", 42));

    private LogRecord record = null;
    private String fullLogMessage = null;
    private String logMessageWithoutcustomFields = null;
    private static String hostName;
    private JsonObjectBuilder fieldsBuilderWithCustomFields;

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown-host";
        }
    }

    public static Exception buildException(final String message, final Throwable cause,
            final StackTraceElement...stackTrace) {
        final Exception result = new Exception(message, cause);
        result.setStackTrace(stackTrace);
        return result;
    }

    /**
     *
     */
    @Before
    public void setUp() {

        long millis = System.currentTimeMillis();
        record = new LogRecord(Level.ALL, "Junit Test");
        record.setLoggerName(LogstashUtilFormatter.class.getName());
        record.setSourceClassName(LogstashUtilFormatter.class.getName());
        record.setSourceMethodName("testMethod");
        record.setMillis(millis);
        record.setThrown(ex);

        fullLogMessage = createFullMessage(millis);
        logMessageWithoutcustomFields = createMessageWithoutCustomFields(millis);
    }

	private String createFullMessage(long millis) {
		JsonObjectBuilder builder = Json.createBuilderFactory(null).createObjectBuilder();
        addCommonElements(millis, builder);

        fieldsBuilderWithCustomFields = Json.createBuilderFactory(null).createObjectBuilder();
        addCommonFields(millis, fieldsBuilderWithCustomFields);
        fieldsBuilderWithCustomFields.add("foo", "bar");
        fieldsBuilderWithCustomFields.add("baz", "foobar");

        builder.add("@fields", fieldsBuilderWithCustomFields);

        JsonArrayBuilder tagsBuilder = Json.createArrayBuilder();
        tagsBuilder.add("foo");
        tagsBuilder.add("bar");
        builder.add("@tags", tagsBuilder.build());

        return builder.build().toString() + "\n";
	}

	private String createMessageWithoutCustomFields(long millis) {
		JsonObjectBuilder builder = Json.createBuilderFactory(null).createObjectBuilder();
        addCommonElements(millis, builder);

        JsonObjectBuilder fieldsBuilder = Json.createBuilderFactory(null).createObjectBuilder();
        addCommonFields(millis, fieldsBuilder);

        builder.add("@fields", fieldsBuilder);

        JsonArrayBuilder tagsBuilder = Json.createArrayBuilder();
        tagsBuilder.add("foo");
        tagsBuilder.add("bar");
        builder.add("@tags", tagsBuilder.build());

        return builder.build().toString() + "\n";
	}

	private void addCommonElements(long millis, JsonObjectBuilder builder) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat(LogstashUtilFormatter.DATE_FORMAT);
        String dateString = dateFormat.format(new Date(millis));
        builder.add("@timestamp", dateString);
        builder.add("message", "Junit Test");
        builder.add("source", LogstashUtilFormatter.class.getName());
        builder.add("source_host", hostName);
	}

	private void addCommonFields(long millis, JsonObjectBuilder fieldsBuilder) {
		fieldsBuilder.add("timestamp", millis);
        fieldsBuilder.add("level", Level.ALL.toString());
        fieldsBuilder.add("line_number", ex.getStackTrace()[0].getLineNumber());
        fieldsBuilder.add("class", LogstashUtilFormatter.class.getName());
        fieldsBuilder.add("method", "testMethod");
        fieldsBuilder.add("exception_class", ex.getClass().getName());
        fieldsBuilder.add("exception_message", ex.getMessage());
        fieldsBuilder.add("stacktrace", EXPECTED_EX_STACKTRACE);
	}

    @Test
    public void testFormatWithEmptyFields() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "");
        LogstashUtilFormatter instance = new LogstashUtilFormatter();

        String result = instance.format(record);
        assertEquals(logMessageWithoutcustomFields, result);
    }

    /**
     * Test of format method, of class LogstashFormatter.
     */
    @Test
    public void testFormat() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        LogstashUtilFormatter instance = new LogstashUtilFormatter();

        String result = instance.format(record);
        assertEquals(fullLogMessage, result);
    }

    /**
     * Test of encodeFields method, of class LogstashFormatter.
     */
    @Test
    public void testEncodeFields() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
    	LogstashUtilFormatter instance = new LogstashUtilFormatter();

        JsonObjectBuilder result = instance.encodeFields(record);
        assertEquals(fieldsBuilderWithCustomFields.build().toString(), result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfo() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        final String expected = Json.createBuilderFactory(null).createObjectBuilder()
            .add("exception_class", ex.getClass().getName())
            .add("exception_message", ex.getMessage())
            .add("stacktrace", EXPECTED_EX_STACKTRACE)
            .build().toString();

        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        instance.addThrowableInfo(record, result);
        assertEquals(expected, result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoNoThrowableAttached() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        instance.addThrowableInfo(new LogRecord(Level.OFF, hostName), result);
        assertEquals("{}", result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoThrowableAttachedButWithoutSourceClassName() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        final String expected = Json.createBuilderFactory(null).createObjectBuilder()
                .add("exception_message", ex.getMessage())
                .add("stacktrace", EXPECTED_EX_STACKTRACE)
                .build().toString();

        record.setSourceClassName(null);

        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        instance.addThrowableInfo(record, result);
        assertEquals(expected, result.build().toString());
    }

    /**
     * Test of addThrowableInfo method, of class LogstashFormatter.
     */
    @Test
    public void testAddThrowableInfoThrowableAttachedButWithoutMessage() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        final Exception ex2 = buildException(null, null, new StackTraceElement[0]);
        record.setThrown(ex2);

        final String expected = Json.createBuilderFactory(null).createObjectBuilder()
                .add("exception_class", ex2.getClass().getName())
                .add("stacktrace", "java.lang.Exception\n")
                .build().toString();

        JsonObjectBuilder result = Json.createBuilderFactory(null).createObjectBuilder();
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        instance.addThrowableInfo(record, result);
        assertEquals(expected, result.build().toString());
    }

    /**
     * Test of getLineNumber method, of class LogstashFormatter.
     */
    @Test
    public void testGetLineNumber() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
    	LogstashUtilFormatter instance = new LogstashUtilFormatter();
        int result = instance.getLineNumber(record);
        assertEquals(ex.getStackTrace()[0].getLineNumber(), result);
    }

    /**
     * Test of getLineNumber method, of class LogstashFormatter.
     */
    @Test
    public void testGetLineNumberNoThrown() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
    	LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals(0, instance.getLineNumber(new LogRecord(Level.OFF, "foo")));
    }

    /**
     * Test of getLineNumberFromStackTrace method, of class LogstashUtilFormatter.
     */
    @Test
    public void testGetLineNumberFromStackTrace() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
    	LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals(0, instance.getLineNumberFromStackTrace(new StackTraceElement[0]));
        assertEquals(0, instance.getLineNumberFromStackTrace(new StackTraceElement[]{null}));
    }

    /**
     * Test of addValue method, of class LogstashUtilFormatter.
     */
    @Test
    public void testAddValue() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        JsonObjectBuilder builder = Json.createBuilderFactory(null).createObjectBuilder();
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        instance.addValue(builder, "key", "value");
        assertEquals("{\"key\":\"value\"}", builder.build().toString());
    }

    /**
     * Test of addValue method, of class LogstashUtilFormatter.
     */
    @Test
    public void testAddNullValue() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        JsonObjectBuilder builder = Json.createBuilderFactory(null).createObjectBuilder();
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        instance.addValue(builder, "key", null);
        assertEquals("{\"key\":\"null\"}", builder.build().toString());
    }

    @Test
    public void testFormatMessageWithSquigglyFormat() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("{0} %s");
        record.setParameters(new Object[] { "hi" });
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("hi %s", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithSquigglyFormatAndNullParameters() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("{0}");
        record.setParameters(null);
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("{0}", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithSquigglyFormatAndEmptyParameters() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("{0}");
        record.setParameters(new Object[0]);
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("{0}", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithBogusSquigglyFormatAndOkPercentFormat() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        // this will fail the squiggly formatting, and fall back to % formatting
        record.setMessage("{0'}' %s");
        record.setParameters(new Object[] { "hi" });
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("{0'}' hi", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithPercentFormat() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("%s");
        record.setParameters(new Object[] { "hi" });
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("hi", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithPercentFormatAndNullParameters() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("%s");
        record.setParameters(null);
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("%s", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithPercentFormatAndEmptyParameters() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("%s");
        record.setParameters(new Object[0]);
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("%s", instance.formatMessage(record));
    }

    @Test
    public void testFormatMessageWithBogusPercentFormat() {
    	System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.tags", "foo,bar");
        System.setProperty("net.logstash.logging.formatter.LogstashUtilFormatter.customfields", "foo:bar,baz:foobar");
        record.setMessage("%0.5s");
        record.setParameters(new Object[] { "hi" });
        LogstashUtilFormatter instance = new LogstashUtilFormatter();
        assertEquals("%0.5s", instance.formatMessage(record));
    }
}
