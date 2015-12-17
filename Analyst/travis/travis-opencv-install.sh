#!/bin/bash
set -ex

# Install the OpenCV JAR file to the local repo
mvn install:install-file -Dfile=lib/face/opencv-2412.jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=2.4.12 -Dpackaging=jar

