#!/bin/bash
umask 0002

if [[ -f "config/service.env" ]]
then
    source config/service.env
fi

export JAVA_OPTS="`./jvm_options_parser jvm.options`"
echo "Java Opts: ${JAVA_OPTS}"
java ${JAVA_OPTS} -jar service.jar

