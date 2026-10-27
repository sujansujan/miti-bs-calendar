#!/bin/sh

##############################################################################
# Gradle start up script for POSIX generated for Miti BS Calendar
##############################################################################

APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    mkdir -p "$APP_HOME/gradle/wrapper"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
    else
        echo "ERROR: gradle-wrapper.jar is missing and neither curl nor wget is installed." >&2
        exit 1
    fi
fi

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" \
    -Xmx64m -Xms64m \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
