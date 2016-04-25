#!/bin/bash
set -ex

pushd ..
git clone git@github.com:Zorroa/Java-common.git
cd Java-common
mvn clean ; mvn install
popd
