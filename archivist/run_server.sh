#!/bin/sh
#This script the docker file entrypoint.

if [ -n "${GAE_SERVICE}" ]
then
  echo "Downloading config files from GCS secret bucket."
  mkdir -p /config
  /root/google-cloud-sdk/bin/gsutil cp -r "gs://${GCLOUD_PROJECT}-zorroa-configuration/${GAE_SERVICE}-config/*" /config
fi

MEM=`cat /proc/meminfo | grep MemTotal | awk '{printf ("%0.fm", $2/2/1280)}'`
JAVA_OPTS="$JAVA_OPTS -Xms$MEM"
JAVA_OPTS="$JAVA_OPTS -Xmx$MEM"

java $JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=60 -Dloader.path=/config/ext -Djava.security.egd=file:/dev/./urandom -jar /service/archivist.jar "$@"
