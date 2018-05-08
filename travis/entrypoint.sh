#!/bin/bash
set -e

# This is the actual test script run inside the docker image.
# It first builds the plugin-sdk repo and copies the java archives
# to their respective destination in the server repo. It then traverses
# the the server repo which is being built and unit tested.

echo -e "\033[0;31mBuilding zorroa-plugin-sdk\033[0m"
pushd zorroa-plugin-sdk
mvn --quiet clean install -Dmaven.test.skip=true
popd

echo -e "\033[0;31mBuilding zorroa-server\033[0m"
pushd zorroa-server
# This is a little piece of hackery. The symlink in the zorroa-server repo
#     unittest/shared -> ../../zorroa-plugin-sdk/unittest/shared
# is broken in the process of mounting the Docker volumes of the two external repos.
# The following recreates it from within the complected Docker container.
rm unittest/shared
ln -s ../../zorroa-plugin-sdk/unittest/shared
# Continue to building and testing
mvn --quiet clean install
popd


