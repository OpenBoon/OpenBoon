#!/bin/bash
mvn clean
rm -rf shared/plugins/*
rm plugins/*.zip
cp ../../zorroa-plugin-sdk/dist/*.zip plugins
