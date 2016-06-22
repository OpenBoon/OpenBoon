#!/bin/bash
#
# Usage: install-jar.sh <jar-file> <src-dir> <dst-dir>
#
# If the destination directory exists, then remove the uncompressed plugin,
# and copy the zip file so it will be uncompressed when Analyst restarts.
#set -x

if [ $# -ne 3 ]; then
    echo "Usage: $0 <jar-file> <src-dir> <dst-dir>"
    exit 1
fi

JAR=$1
SRC_DIR=$2
DST_DIR=$3

if [ -d "${DST_DIR}" ]; then
    rm -f "${DST_DIR}/${JAR}"
    cp "${SRC_DIR}/${JAR}" "${DST_DIR}"
fi
