#!/bin/bash
#This script the docker file entrypoint.

if [[ -f "/config/archivist.env" ]]
then
    source /config/archivist.env
fi

export ZORROA_JAVA_OPTS=`./jvm_options_parser /config/jvm.options`
java ${ZORROA_JAVA_OPTS} \
-Djava.awt.headless=true \
-Djava.security.egd=file:/dev/./urandom \
${JVM_OPTIONS} -jar /service/archivist.jar "$@"
