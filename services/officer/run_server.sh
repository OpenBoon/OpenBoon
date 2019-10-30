#!/bin/sh
umask 0002

if [ "x${ZORROA_STORAGE_PATH}" != "x" ]; then
    JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${ZORROA_STORAGE_PATH}"
fi

echo "Java Opts: ${JAVA_OPTS}"
java -Xms512m -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=60 -XX:MaxRAMPercentage=60 -Xlog:gc $JAVA_OPTS -jar officer.jar -port 7081
