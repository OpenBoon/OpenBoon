#!/bin/bash

docker build . -t boonai/jvm-build
dockere push boonai/jvm-build

