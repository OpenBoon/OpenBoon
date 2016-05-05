#!/bin/bash
mvn  package -Dmaven.test.skip=true
java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -jar target/analyst.jar

