#!/bin/bash
#This script the docker file entrypoint.

if [ -f "/config/archivist.env" ]
then
    source /config/archivist.env
fi

if [ -n "${ZORROA_ARCHIVIST_EXT}" ]
then
   extArray=$(echo ${ZORROA_ARCHIVIST_EXT} | tr ";" "\n")
   for ext in $extArray
   do
      echo "Activating Archivist ${ext} extension"
      cp /extensions/inactive/${ext} /extensions/active
   done
fi

# DEPRECATED: Should be removed once all instances are updated to use the next block of code.
if [ -n "${GAE_SERVICE}" ]
then
  echo "Downloading config files from GCS secret bucket."
  gsutil cp -r "gs://${GCLOUD_PROJECT}-zorroa-configuration/${GAE_SERVICE}-config/*" /config
fi

if [ -n "${GCS_CONFIGURATION_BUCKET}" ]
then
  echo "Downloading config files from GCS secret bucket."
  gsutil cp -r "gs://${GCS_CONFIGURATION_BUCKET}/*" /config
fi


export ZORROA_JAVA_OPTS=`./jvm_options_parser /config/jvm.options`
java ${ZORROA_JAVA_OPTS} \
-Djava.awt.headless=true \
-Dloader.path=/extensions/active \
-Djava.security.egd=file:/dev/./urandom \
${JVM_OPTIONS} -jar /service/archivist.jar "$@"
