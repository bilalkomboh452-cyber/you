#!/bin/sh
set -e
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then echo "Already present."; exit 0; fi
mkdir -p gradle/wrapper
curl -fsSL "https://raw.githubusercontent.com/gradle/gradle/v8.6.0/gradle/wrapper/gradle-wrapper.jar" -o "$WRAPPER_JAR"
echo "Done: $(du -sh $WRAPPER_JAR)"
