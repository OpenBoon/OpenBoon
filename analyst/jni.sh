#!/bin/bash
#
# Generate JNI header or compile JNI library for a java class

#set -ex

Usage ()
{
    echo "Usage:   jni javah  <ClassName> - builds JNI header in target dir"
    echo "         jni jnilib <ClassName> - builds & links JNI library"
    echo "Options: -lfoo.a -lbar.so       - links libfoo.a into JNI library"
}

if [ "$#" -lt "2" ]; then
    Usage
    exit 1
fi

# Action = javah, jnilib
COMMAND=$1

# JNI Class name with native methods, expects e.g. caffe.CaffeIngestor
CLASS_NAME=$2

shift
shift
while (( "$#" )); do
    ARG=$1
    PREFIX=${1:0:2}
    if [ "${PREFIX}" == "-I" ] || [ "${PREFIX}" == "-D" ]; then
        EXTRA_INCLUDES+="${ARG} "
    elif [ "${PREFIX}" == "-l" ] || [ "${PREFIX}" == "-L" ]; then
        EXTRA_LIBS+="${ARG} "
    fi
    shift
done


#
# Variables and directories
#

# Project name
PROJECT_NAME=com.zorroa.ingestors

# Output directory root
TARGET_ROOT_DIR=target/jni

# Location of source inputs
SRC_DIR=src/main

# Last component of class name
CLASS_NAME_EXT=${CLASS_NAME##*.}

# Path version of local class name
CLASS_PATH=${CLASS_NAME//.//}

# Directory component of local class path
CLASS_DOMAIN=$(dirname "$CLASS_PATH")

# Fully qualified class name, when in src/main
FULL_CLASS_NAME=${PROJECT_NAME}.${CLASS_NAME}

# Fully qualified domain name, without the final class name
FULL_CLASS_DOMAIN=${PROJECT_NAME}.${CLASS_DOMAIN}

# Path version of fully qualified class name, for sources & targets
DOMAIN_PATH_DIR=${FULL_CLASS_DOMAIN//.//}

# Output location for build results
TARGET_DIR=${TARGET_ROOT_DIR}/${CLASS_DOMAIN}

#
# Build commands
#

# Create output directory under target, mirroring source
mkdir -p ${TARGET_DIR}

if [ "${COMMAND}" == "javah" ]; then
    # Create the JNI header using javah, must be in SRC_DIR
    JNI_HEADER=${TARGET_DIR}/${CLASS_NAME_EXT}.h
    echo "Generating ${JNI_HEADER}"
    pushd ${SRC_DIR}/java
    javah -jni ${EXTRA_INCLUDES} -o ../../../${JNI_HEADER} ${FULL_CLASS_NAME}
    popd
elif [ "${COMMAND}" == "jnilib" ]; then
    # Compile the JNI object files into a library
    SRC_FILE=${SRC_DIR}/C++/${DOMAIN_PATH_DIR}/${CLASS_NAME_EXT}.cpp
    INCLUDE_DIRS="-I/System/Library/Frameworks/JavaVM.framework/Headers -I/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/include/c++/v1 -I${TARGET_DIR} -I/usr/include"
    echo "Compiling ${SRC_FILE}"
    cc -c ${EXTRA_INCLUDES} -o ${TARGET_DIR}/${CLASS_NAME_EXT}.o ${INCLUDE_DIRS} -nostdlibinc ${SRC_FILE}
    JNI_LIB=${TARGET_DIR}/lib${CLASS_NAME_EXT}.jnilib
    echo "Linking ${JNI_LIB}"
    cc -dynamiclib ${EXTRA_LIBS} -o ${JNI_LIB} ${TARGET_DIR}/${CLASS_NAME_EXT}.o
    ./jni_relink.py ${JNI_LIB}
else
    Usage
    exit 1
fi
