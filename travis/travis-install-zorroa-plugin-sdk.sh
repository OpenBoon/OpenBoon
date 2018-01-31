#!/bin/bash

# Simply clones the plugin-sdk into the parent folder.
# There is no point in building it here and now, since
# we need it to install its binaries inside the Docker image.

set -ex

pushd ..
git clone git@github.com:Zorroa/zorroa-plugin-sdk.git --quiet
popd
