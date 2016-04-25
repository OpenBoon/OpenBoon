#!/bin/bash
set -ex

pushd ..

git clone https://github.com/sqlboy/TwelveMonkeys
cd TwelveMonkeys
mvn clean install

popd
