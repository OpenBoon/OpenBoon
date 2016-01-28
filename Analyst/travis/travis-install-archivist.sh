#!/bin/bash
set -ex

pushd ..
git clone git@github.com:Zorroa/Archivist.git
cd Archivist
travis/travis-install-image-drivers.sh
mvn clean
mvn package -Dmaven.test.skip=true
popd
