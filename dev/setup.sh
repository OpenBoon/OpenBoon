#!/bin/bash
# Download the zorroa development keys and store within the zmlp-config volume.
# You must have access to the gs://zmlp-dev-keys bucket.

gsutil cp gs://zmlp-dev-keys/* zmlp-config

docker volume create zmlp-config
docker run -it -v zmlp-config:/zmlp-config -v ${PWD}/zmlp-config:/new-zmlp-config \
  ubuntu:disco /bin/bash -v -c 'cp /new-zmlp-config/* /zmlp-config'





