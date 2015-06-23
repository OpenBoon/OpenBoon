#!/bin/bash
#
# Generate JNI header or compile JNI library for a java class

#set -x

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

# JNI Class name with native methods
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
PROJECT_NAME=com.zorroa.archivist

# Output directory root
TARGET_ROOT_DIR=target/jni

# Location of source inputs
SRC_DIR=src/main

# Fully qualified class name, when in src/main
FULL_CLASS_NAME=${PROJECT_NAME}.processors.${CLASS_NAME}

# Path version of fully qualified class name, for sources & targets
CLASS_PATH_DIR=${FULL_CLASS_NAME//.//}

# Output location for build results
TARGET_DIR=${TARGET_ROOT_DIR}/${CLASS_PATH_DIR}

#
# Build commands
#

# Create output directory under target, mirroring source
mkdir -p ${TARGET_DIR}

if [ "${COMMAND}" == "javah" ]; then
    # Create the JNI header using javah, must be in SRC_DIR
    JNI_HEADER=${TARGET_DIR}/${CLASS_NAME}.h
    echo "Generating ${JNI_HEADER}"
    pushd ${SRC_DIR}/java
    javah -jni ${EXTRA_INCLUDES} -o ../../../${JNI_HEADER} ${FULL_CLASS_NAME}
    popd
elif [ "${COMMAND}" == "jnilib" ]; then
    # Compile the JNI object files into a library
    SRC_FILE=${SRC_DIR}/C++/${CLASS_PATH_DIR}.cpp
    INCLUDE_DIRS="-I/System/Library/Frameworks/JavaVM.framework/Headers -I${TARGET_DIR}"
    echo "Compiling ${SRC_FILE}"
    cc -c ${EXTRA_INCLUDES} -o ${TARGET_DIR}/${CLASS_NAME}.o ${INCLUDE_DIRS} ${SRC_FILE}
    JNI_LIB=${TARGET_DIR}/lib${CLASS_NAME}.jnilib
    echo "Linking ${JNI_LIB}"
    cc -dynamiclib ${EXTRA_LIBS} -o ${JNI_LIB} ${TARGET_DIR}/${CLASS_NAME}.o
else
    Usage
    exit 1
fi
