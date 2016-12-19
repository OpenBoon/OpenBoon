#!/bin/sh

if [ "x$ARCHIVIST_MIN_MEM" = "x" ]; then
    ARCHIVIST_MIN_MEM=256m
fi
if [ "x$ARCHIVIST_MAX_MEM" = "x" ]; then
    ARCHIVIST_MAX_MEM=2g
fi
if [ "x$ARCHIVIST_HEAP_SIZE" != "x" ]; then
    ARCHIVIST_MIN_MEM=$ARCHIVIST_HEAP_SIZE
    ARCHIVIST_MAX_MEM=$ARCHIVIST_HEAP_SIZE
fi

# min and max heap sizes should be set to the same value to avoid
# stop-the-world GC pauses during resize, and so that we can lock the
# heap in memory on startup to prevent any of it from being swapped
# out.
JAVA_OPTS="$JAVA_OPTS -Xms${ARCHIVIST_MIN_MEM}"
JAVA_OPTS="$JAVA_OPTS -Xmx${ARCHIVIST_MAX_MEM}"

# new generation
if [ "x$ARCHIVIST_HEAP_NEWSIZE" != "x" ]; then
    JAVA_OPTS="$JAVA_OPTS -Xmn${ARCHIVIST_HEAP_NEWSIZE}"
fi

# max direct memory
if [ "x$ARCHIVIST_DIRECT_SIZE" != "x" ]; then
    JAVA_OPTS="$JAVA_OPTS -XX:MaxDirectMemorySize=${ARCHIVIST_DIRECT_SIZE}"
fi

# set to headless, just in case
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"

# Force the JVM to use IPv4 stack
if [ "x$ARCHIVIST_USE_IPV4" != "x" ]; then
  JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
fi

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
