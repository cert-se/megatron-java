#!/bin/sh

# Executes Megatron in production. The following directories are used:
#   * /usr/local/megatron
#   * /var/megatron
#   * /etc/megatron

if [ $(whoami) != "u_megatron" ]; then
  echo "Error: This script must be executed by the u_megatron user."
  echo "Solution: sudo su u_megatron + rerun this script"
  exit 1
fi

# Use UTF-8 (e.g. in generated filenames) 
export LC_ALL=en_US.UTF-8

#export SITIC_JAVA=/usr/local/jdk-1.5.0/bin/java
#export SITIC_JAVA=/usr/local/jdk-1.5.0/jre/bin/java
export SITIC_JAVA=/usr/local/jdk-1.7.0/jre/bin/java
export SITIC_JAVA_OPTIONS="-server -Xmx512M"
# export SITIC_JCONSOLE_OPTIONS="-Dcom.sun.management.jmxremote.port=51010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
export SITIC_JCONSOLE_OPTIONS=
export SITIC_LIB=/usr/local/megatron/lib
export SITIC_CONF=/etc/megatron:/etc/megatron/hibernate-mapping
export SITIC_HIBERNATE_CLASSPATH=$SITIC_CONF:$SITIC_LIB/hibernate3.jar:$SITIC_LIB/activation.jar:$SITIC_LIB/slf4j-api-1.5.6.jar:$SITIC_LIB/antlr-2.7.6.jar:$SITIC_LIB/commons-collections-3.2.1.jar:$SITIC_LIB/dom4j-1.6.1.jar:$SITIC_LIB/javassist.jar:$SITIC_LIB/jta-1.1.jar:$SITIC_LIB/slf4j-log4j12-1.5.6.jar
export SITIC_CLASSPATH=$SITIC_HIBERNATE_CLASSPATH:$SITIC_LIB/sitic-megatron.jar:$SITIC_LIB/log4j.jar:$SITIC_LIB/mysql-connector.jar:$SITIC_LIB/geoip.jar:$SITIC_LIB/mail.jar:$SITIC_LIB/rome.jar:$SITIC_LIB/jdom.jar:$SITIC_LIB/dnsjava.jar:$SITIC_LIB/joda-time.jar:$SITIC_LIB/commons-net.jar

ALL_PARAMS=$*
SKIP_LOCKFILE=$(echo $ALL_PARAMS | egrep '.*--help.*|.*--version.*|.*--list-prios.*|.*--list-jobs.*|.*--job-info.*|.*--ui-org.*' | wc -l | sed 's/ //g')

if [ $SKIP_LOCKFILE == "0" ] ; then
  if test -f /var/megatron/megatron.pid ; then
    echo "Megatron already started (lock-file '/var/megatron/megatron.pid' exists)."

    # check if there is a java process running, if not the lockfile is probably stale
    MEGATRON_RUNNING=`ps auxwww|grep ".*java.*Megatron.*"|grep -v grep| wc -l | sed 's/ //g'`
    if [ $MEGATRON_RUNNING = 0 ]; then
      echo ""
      echo "Megatron seems to have a stale lock-file. No java processes are running"
      echo "run 'rm /var/megatron/megatron.pid' and try again, it should be safe..."
      echo ""
      exit 100
    else
      exit 101
    fi
  else
    echo $$ > /var/megatron/megatron.pid
    echo `date`: "Megatron Starts (with lock-file)." PID=`more /var/megatron/megatron.pid`
  fi
else
    echo `date`: "Megatron Starts." PID=$$
fi

$SITIC_JAVA $SITIC_JAVA_OPTIONS $SITIC_JCONSOLE_OPTIONS -cp $SITIC_CLASSPATH -Dfile.encoding=UTF-8 -Dmegatron.configfile=/etc/megatron/megatron-globals.properties Megatron $ALL_PARAMS
MEGATRON_EXIT_CODE=$?
echo `date`: "Megatron Finished."

if [ $SKIP_LOCKFILE == "0" ] ; then
  rm /var/megatron/megatron.pid
fi

# Return exit code to calling shell script
$(exit $MEGATRON_EXIT_CODE)
