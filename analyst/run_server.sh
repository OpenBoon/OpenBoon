#!/bin/sh
if [ -n "${GAE_SERVICE}" ]
then
  echo "Downloading config files from GCS secret bucket."
  mkdir -p /config
  /root/google-cloud-sdk/bin/gsutil cp -r "${GCLOUD_PROJECT}-zorroa-configuration/{GAE_SERVICE}-config/*" /config
fi
java -Dloader.path=/config/ext -Djava.security.egd=file:/dev/./urandom -jar /service/analyst.jar "$@"
