#!/bin/bash
rm -rf shared/plugins/*
rm -rf plugins
mkdir plugins
cp ../../zorroa-plugin-sdk/dist/*.zip plugins
