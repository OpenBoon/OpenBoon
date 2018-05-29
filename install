#!/bin/bash

if [[ $1 == 'test' ]]
then
    mvn clean install
else
    mvn clean install -Dmaven.test.skip=true
fi
