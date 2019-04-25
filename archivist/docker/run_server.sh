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

if [ -n "${GAE_SERVICE}" ]
then
  echo "Downloading config files from GCS secret bucket."
  gsutil cp -r "gs://${GCLOUD_PROJECT}-zorroa-configuration/${GAE_SERVICE}-config/*" /config
fi


export ZORROA_JAVA_OPTS=`./jvm_options_parser /config/jvm.options`
java ${ZORROA_JAVA_OPTS} \
-Djava.awt.headless=true \
-Dloader.path=/extensions/active \
-Djava.security.egd=file:/dev/./urandom \
-jar /service/archivist.jar "$@"
