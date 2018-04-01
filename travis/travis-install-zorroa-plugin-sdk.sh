#!/bin/bash
set -ex

pushd ..

git clone git@github.com:Zorroa/zorroa-plugin-sdk.git
cd zorroa-plugin-sdk
mvn clean ; mvn install

popd
