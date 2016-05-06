#!/bin/bash
mvn  package -Dmaven.test.skip=true
java -jar target/archivist.jar
