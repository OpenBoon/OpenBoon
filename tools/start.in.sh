#!/bin/sh
# For Oracle Driver or any other software that uses /dev/random
# Using /dev/urandom is a lot faster
export JAVA_TOOL_OPTIONS="-Djava.security.egd=file:/dev/./urandom"

# FOR ELASTIC NODES:
# min and max heap sizes should be set to the same value to avoid
# stop-the-world GC pauses during resize, and so that we can lock the
# heap in memory on startup to prevent any of it from being swapped
# out.

#
# For linux we detect the mem and default to 1/2 the memory
#

if [[ "$OSTYPE" == "linux-gnu" ] && [ "$APP_NAME" == "archivist" ]]; then
    MEM=`cat /proc/meminfo | grep MemTotal | awk '{printf ("%0.fm", $2/2/1024)}'`
    JAVA_OPTS="$JAVA_OPTS -Xms$MEM"
    JAVA_OPTS="$JAVA_OPTS -Xmx$MEM"
else
    JAVA_OPTS="$JAVA_OPTS -Xms1g"
    JAVA_OPTS="$JAVA_OPTS -Xmx1g"
fi

# set to headless, just in case
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"

JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC"
JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC"

JAVA_OPTS="$JAVA_OPTS -XX:CMSInitiatingOccupancyFraction=75"
JAVA_OPTS="$JAVA_OPTS -XX:+UseCMSInitiatingOccupancyOnly"

# GC logging options
if [ "x$ARCHIVIST_USE_GC_LOGGING" != "x" ]; then
  JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails"
  JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCTimeStamps"
  JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDateStamps"
  JAVA_OPTS="$JAVA_OPTS -XX:+PrintClassHistogram"
  JAVA_OPTS="$JAVA_OPTS -XX:+PrintTenuringDistribution"
  JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCApplicationStoppedTime"
  JAVA_OPTS="$JAVA_OPTS -Xloggc:/var/log/elasticsearch/gc.log"
fi

# Disables explicit GC
JAVA_OPTS="$JAVA_OPTS -XX:+DisableExplicitGC"

# Ensure UTF-8 encoding by default (e.g. filenames)
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"

# Unset Zorroa client env vars in case they cause troubles with
# defaults when using the JAVA SDK client.
unset ZORROA_USER
unset ZORROA_ARCHIVIST_URL
unset ZORROA_HMAC_PATH
unset ZORROA_HMAC_KEY
unset ZORROA_USER
