#!/bin/sh
if [ -n "${GCS_CONFIG}" ]
then
  echo "Downloading config files from GCS secret bucket."
  /root/google-cloud-sdk/bin/gsutil cp -r "${GCS_CONFIG}" ./config
fi
java -Dloader.path=/service/ext -Djava.security.egd=file:/dev/./urandom -jar /service/app.jar "$@"