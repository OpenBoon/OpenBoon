#!/bin/bash

if [[ -f "config/service.env" ]]
then
    source config/service.env
fi

JAVA_OPTS=`./jvm_options_parser config/jvm.options`
java ${JAVA_OPTS} -jar service.jar "$@"

