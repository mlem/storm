#!/bin/bash
#
# Launch Zomboid Storm
#
###############################################################################

# Assume launch script is in 'pz/storm/bin' directory and declare storm and zomboid root directories
STORM_HOME=$(readlink -f "$(pwd)/../")
PZ_HOME=$(readlink -f "${STORM_HOME}/../")
cd "${PZ_HOME}" || exit 1
PZ_HOME=$(pwd)

if [ ! -f "$PZ_HOME/ProjectZomboid64" ] && [ ! -f "$PZ_HOME/ProjectZomboid32" ]; then
  echo -e "\nERROR: Unable to find Project Zomboid installation directory." \
    "Searched in directory '${PZ_HOME}' as relative to Storm installation directory '${STORM_HOME}/../'\n"
	exit 1
fi

# path to local game directory where logs, mods and configs are stored
LOCAL_PZ_HOME="${HOME}/Zomboid"

exportGameJava() {
  if "$1/jre64/bin/java" -version >/dev/null 2>&1; then
    echo "64-bit java detected in game directory"
    export JAVA_CMD="$1/jre64/bin/java"
    return 0
  elif "${INSTDIR}/jre/bin/java" -client -version >/dev/null 2>&1; then
    echo "32-bit java detected game directory"
    export JAVA_CMD="$1/jre/bin/java"
    return 0
  else
    echo -e "\nERROR: Couldn't determine 32/64 bit of java in game directory\n"
    return 1
  fi
}

# Create local game directory if it doesn't exist and set directory permission to a+rwx,g-w,o-w
[[ ! -d "${LOCAL_PZ_HOME}" ]] && mkdir -m 0755 "${LOCAL_PZ_HOME}"

if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVA_CMD="$JAVA_HOME/jre/sh/java"
  else
    JAVA_CMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVA_CMD" ]; then
    exportGameJava $PZ_HOME
    if [ $? != 0 ]; then
      echo -e "\nERROR: JAVA_HOME is set to an invalid directory ($JAVA_HOME) and no Java distribution" \
        "found in game directory.\nPlease reinstall Project Zomboid or set the JAVA_HOME variable" \
        "to match the location of your Java installation if you want to run Storm" \
        "with a custom Java distribution\n"
      exit 1
    fi
  fi
else
  exportGameJava $PZ_HOME
  if [ $? != 0 ]; then
    JAVA_CMD="java"
    which java >/dev/null 2>&1 || echo -e "\nERROR: JAVA_HOME is not set and no 'java' command" \
      "could be found in your PATH.\nPlease set the JAVA_HOME variable in your environment" \
      "to match the location of your Java installation.\n"
    exit 1
  fi
fi

# This is the Java version we want
JAVA_TARGET_VERSION="17"

# Determine which Java version is used
JAVA_VERSION_INFO=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2}')
JAVA_VERSION=$(echo "$JAVA_VERSION_INFO" | cut -d '.' -f -2)

if [ "$JAVA_VERSION" != $JAVA_TARGET_VERSION ]; then
  echo -e "\nERROR: JAVA_HOME points to wrong Java version ($JAVA_VERSION)." \
    "\nPlease set the JAVA_HOME variable in your environment to match the" \
    "location of Java version $JAVA_TARGET_VERSION installation.\n"
  exit 1
fi

# Path to Storm jar library directory
LIB_PATH="${STORM_HOME}/lib"

CLASSPATH="${LIB_PATH}/storm-@stormVersion@.jar:\
${LIB_PATH}/asm-9.1.jar:\
${LIB_PATH}/asm-analysis-9.1.jar:\
${LIB_PATH}/asm-tree-9.1.jar:\
${LIB_PATH}/asm-util-9.1.jar:\
${LIB_PATH}/guava-31.1-jre.jar:\
${LIB_PATH}/log4j-api-2.17.2.jar:\
${LIB_PATH}/log4j-core-2.17.2.jar:\
${PZ_HOME}:\
${PZ_HOME}/*"

# Project Zomboid properties
PZ_OPTS="-Dzomboid.steam=1 -Dzomboid.znetlog=1"

# Java command-line options
JAVA_OPTS="-XX:-CreateMinidumpOnCrash \
-XX:-OmitStackTraceInFastThrow -Xms1800m -Xmx2048m"

# Java system properties
SYS_PROPS="-Djava.library.path=${PZ_HOME}:${PZ_HOME}/linux64:\
${PZ_HOME}/jre64/lib/amd64 -Dorg.lwjgl.librarypath=${PZ_HOME}"

STORM_VERSION=`cat versionFile.txt`

echo "Launching Zomboid Storm $STORM_VERSION..."

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $PZ_OPTS $JAVA_OPTS $SYS_PROPS -classpath "$CLASSPATH" io.pzstorm.storm.core.StormLauncher

# Execute Java command to launch Storm
exec "$JAVA_CMD" "$@"
