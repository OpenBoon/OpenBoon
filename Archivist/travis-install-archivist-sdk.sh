#!/bin/bash
set -ex

pushd ..
git clone https://github.com/Zorroa/ArchivistSDK.git
cd ArchivistSDK
mvn clean ; mvn install
popd
