#!/bin/bash

docker run -it -w /zorroa-server -v ~/.m2:/root/.m2 -v $PWD:/zorroa-server maven:3.5.4-jdk-8 mvn clean install -Dmaven.test.skip=true
cd elasticsearch
./build-docker.sh
cd ../archivist
./build-docker.sh

