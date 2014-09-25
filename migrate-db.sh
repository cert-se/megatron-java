#!/bin/sh

# All paths are relative to the installation directory.

export SITIC_JAVA=$JAVA_HOME/bin/java
#export SITIC_JAVA_OPTIONS="-server -Xmx512M"
export SITIC_JAVA_OPTIONS=
#export SITIC_JCONSOLE_OPTIONS="-Dcom.sun.management.jmxremote.port=51010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
export SITIC_JCONSOLE_OPTIONS=
export SITIC_LIB=lib
export SITIC_DIST=dist
export SITIC_CONF=conf/dev:conf/hibernate-mapping
export SITIC_HIBERNATE_CLASSPATH=$SITIC_CONF:$SITIC_LIB/hibernate3.jar:$SITIC_LIB/activation.jar:$SITIC_LIB/slf4j-api-1.5.6.jar:$SITIC_LIB/antlr-2.7.6.jar:$SITIC_LIB/commons-collections-3.2.1.jar:$SITIC_LIB/dom4j-1.6.1.jar:$SITIC_LIB/javassist.jar:$SITIC_LIB/jta-1.1.jar:$SITIC_LIB/slf4j-log4j12-1.5.6.jar
export SITIC_CLASSPATH=$SITIC_HIBERNATE_CLASSPATH:$SITIC_DIST/sitic-megatron.jar:$SITIC_LIB/log4j.jar:$SITIC_LIB/mysql-connector.jar:$SITIC_LIB/geoip.jar:$SITIC_LIB/mail.jar:$SITIC_LIB/rome.jar:$SITIC_LIB/jdom.jar:$SITIC_LIB/dnsjava.jar:$SITIC_LIB/joda-time.jar:$SITIC_LIB/commons-net.jar
echo `date`: Megatron database migration starts.
$SITIC_JAVA $SITIC_JAVA_OPTIONS $SITIC_JCONSOLE_OPTIONS -cp $SITIC_CLASSPATH -Dmegatron.configfile=conf/dev/megatron-globals.properties OrganizationContactMigrator $*
echo `date`: "Megatron database migration finished."
