#!/bin/bash
set -ex

pushd ..
git clone git@github.com:Zorroa/Archivist.git
cd Archivist
mvn clean
mvn package -Dmaven.test.skip=true
popd
