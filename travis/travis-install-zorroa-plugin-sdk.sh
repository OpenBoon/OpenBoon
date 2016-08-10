#!/bin/bash
set -ex

pushd ..

git clone https://github.com/sqlboy/TwelveMonkeys
cd TwelveMonkeys
mvn clean install

cd ..

git clone git@github.com:Zorroa/ShotgunJava.git
cd ShotgunJava
mvn clean ; mvn install

cd ..

git clone git@github.com:Zorroa/zorroa-plugin-sdk.git
cd zorroa-plugin-sdk
mvn clean ; mvn install

cd ..

git clone git@github.com:Zorroa/FileseqJava.git
cd FileseqJava
mvn clean ; mvn install

popd
