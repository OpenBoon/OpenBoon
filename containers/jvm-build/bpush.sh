#!/bin/bash

docker build . -t boonai/jvm-build
docker push boonai/jvm-build

