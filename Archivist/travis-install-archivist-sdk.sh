#!/bin/bash
set -ex

pushd ..
git clone git@github.com:Zorroa/ArchivistSDK.git
cd ArchivistSDK
mvn clean ; mvn install
popd
