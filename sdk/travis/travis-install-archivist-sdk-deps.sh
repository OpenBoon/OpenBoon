#!/bin/bash
set -ex

pushd ..

git clone git@github.com:Zorroa/ShotgunJava.git
cd ShotgunJava
mvn clean ; mvn install

cd ..

git clone https://github.com/Zorroa/FileseqJava.git
cd FileseqJava
mvn clean ; mvn install

popd
