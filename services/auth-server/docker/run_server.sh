#!/bin/sh
umask 0002

if [[ -f "config/service.env" ]]
then
    source config/service.env
fi

JAVA_OPTS=`./jvm_options_parser jvm.options`
java ${JAVA_OPTS} -jar service.jar

