#!/bin/sh

# Ensure we have somewhere a running version of java
[ -z ${JAVA_HOME} ] && (echo "Must set JAVA_HOME" && exit 1)

JAVA_EXE="${JAVA_HOME}/bin/java"

[ ! -x ${JAVA_EXE} ] && (echo "Must set JAVA_HOME with a real java binary" && exit 1)

SELF=$0

${JAVA_EXE} -Dsun.misc.URLClassPath.disableJarChecking=true -jar ${SELF} "$@"

exit 0