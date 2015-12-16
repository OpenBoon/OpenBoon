#!/bin/bash
set -ex

# Install the OpenCV JAR file to the local repo
mvn install:install-file -Dfile=lib/face/opencv-2412.jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=2.4.12 -Dpackaging=jar

# Install required binary libraries to /usr/local/lib and add symlinks in /usr/local/opt
rm -f /usr/local/lib/libcaffe.so
cp lib/caffe/libcaffe.so /usr/local/lib

rm -f /usr/local/lib/libopencv_java2412.dylib
cp lib/face/libopencv_java2412.dylib /usr/local/lib

pushd lib
tar cf - local | tar xkvf - -C /usr || true
popd

