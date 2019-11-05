#!/bin/sh
umask 0002

if [ "x${ZORROA_STORAGE_PATH}" != "x" ]; then
    JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${ZORROA_STORAGE_PATH}"
fi

JAVA_OPTS=`config/./jvm_options_parser config/jvm.options`
echo "Java Opts: ${JAVA_OPTS}"
java ${JAVA_OPTS} -jar service.jar

