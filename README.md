# JBoss logmanager JSON encoder for Logstash

## Compatibility
This library has been verified and tested using JBoss EAP 7.0,
but is likely to work with later versions as well as WildFly.

It does *not* work on JBoss EAP 6.x.

## Include as a module

First, add it as a module to your JBoss EAP installation.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.1" name="net.logstash">
  <resources>
      <resource-root path="logstash-util-formatter-1.1-SNAPSHOT.jar" />
  </resources>
  <dependencies>
      <module name="javax.json.api"/>
      <module name="org.jboss.logmanager"/>
  </dependencies>
</module>
```

Add a formatter and appender like this:

```xml
<periodic-rotating-file-handler name="LOGSTASH_FILE" autoflush="true">
  <level name="INFO"/>
  <formatter>
    <named-formatter name="LOGSTASH_PATTERN"/>
  </formatter>
  <file relative-to="jboss.server.log.dir" path="logstash.log"/>
  <suffix value=".yyyy-MM-dd"/>
  <append value="true"/>
</periodic-rotating-file-handler>

```
```xml
<formatter name="LOGSTASH_PATTERN">
  <custom-formatter
    class="net.logstash.logging.formatter.LogstashUtilFormatter"
    module="net.logstash"/>
</formatter>
```
Then you can add the appender to your logger(s) like this:
<root-logger>
  <level name="INFO"/>
    <handlers>
      <handler name="CONSOLE"/>
      <handler name="FILE"/>
      <handler name="LOGSTASH_FILE"/>
    </handlers>
</root-logger>

Use it in your logstash configuration like this:
```
input {
  file {
    type => "your-log-type"
    path => "/some/path/to/your/file.log"
    format => "json_event"
  }
}
```

## Custom fields and tags

* By setting the system property `net.logstash.logging.formatter.LogstashUtilFormatter.tags` you may easily add tags,
which let you differentiate between multiple instances running on the same host.

* By setting the system property `net.logstash.logging.formatter.LogstashUtilFormatter.fields`you may easily add extra fields,
the format is `key1:value1,key2:value2`

## Looking for a non-JBoss specific formatter?
This library is based on https://github.com/SYNAXON/logstash-util-formatter,
it has been extended to allow custom fields and get more information from JBoss logging, such as thread names.
The cost of these additions however, is that it became dependent on JBoss EAP/WildFly.
The 'upstream' source uses java.util.logging directly.
