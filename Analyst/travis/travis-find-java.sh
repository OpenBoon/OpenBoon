#/bin/bash
set -x

#JAVA=/Library/Application\ Support/Zorroa/java/jdk1.8.0_65.jdk/Contents/Home/bin/java

set_java_version() {
    JAVA_VERSION=$("${JAVA}" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "Found Java version ${JAVA_VERSION} using ${JAVA}"
}

# Search for the java executable in a few locations
if [ -x "${JAVA_HOME}/bin/java" ]; then
    JAVA="${JAVA_HOME}/bin/java"
else
    JAVA=`which java`
fi

if [ -x "${JAVA}" ]; then
    set_java_version
else
    JAVA_VERSION=0
fi

# Do NOT use the JRE -- it displays alert dialogs when using command-line java executable
#if [[ "${JAVA_VERSION}" < "1.8" ]]; then
#    JAVA_PLUGIN="/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/bin/java"
#    if [ -x "${JAVA_PLUGIN}" ]; then
#        JAVA=${JAVA_PLUGIN}
#        set_java_version
#    fi
#fi

if [[ "${JAVA_VERSION}" < "1.8" ]]; then
    echo "Cannot find Java 1.8+"
    JAVA=
fi
