#!/bin/sh
#
# Copyright 2013 ForgeRock, Inc.
#
# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance
# with the License.
#
# You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
# the specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file
# and include the License file at legal/CDDLv1.0.txt. If applicable, add the
# following below the CDDL Header, with the fields enclosed by brackets []
# replaced by your own identifying information:
# "Portions copyright [year] [name of copyright owner]".
#

JAVA_VER=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
if [ "$JAVA_VER" -lt 17 ]; then
  echo "Java version 1.7 or higher required";
  exit 1;
fi

# clean up left over pid files if necessary
cleanupPidFile() {
  if [ -f "$OPENIDM_PID_FILE" ]; then
    rm -f "$OPENIDM_PID_FILE"
  fi
  trap - EXIT
  exit
}

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

echo "Executing "$PRG"..."

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set OPENIDM_HOME if not already set
[ -z "$OPENIDM_HOME" ] && OPENIDM_HOME=`(cd "$PRGDIR" >/dev/null; pwd)`

# Only set OPENIDM_PID_FILE if not already set
[ -z "$OPENIDM_PID_FILE" ] && OPENIDM_PID_FILE="$OPENIDM_HOME"/.openidm.pid

# Only set OPENIDM_OPTS if not already set
[ -z "$OPENIDM_OPTS" ] && OPENIDM_OPTS="${openidm.options}"

# Set JDK Logger config file if it is present and an override has not been issued
PROJECT_HOME=$OPENIDM_HOME
CLOPTS=""
JPDA=""
while [ "$1" ]; do
    if [ "$1" = "jpda" ]; then
        JPDA=$1
    else
        if [ "$1" = "-p" ] && [ "$2" ]; then
            PROJECT_HOME="$OPENIDM_HOME/$2"
        fi
        CLOPTS="$CLOPTS $1"
    fi
    shift
done
if [ -z "$LOGGING_CONFIG" ]; then
  if [ -r "$PROJECT_HOME"/conf/logging.properties ]; then
    LOGGING_CONFIG="-Djava.util.logging.config.file=$PROJECT_HOME/conf/logging.properties"
  elif [ -r "$OPENIDM_HOME"/conf/logging.properties ]; then
    LOGGING_CONFIG="-Djava.util.logging.config.file=$OPENIDM_HOME/conf/logging.properties"
  else
    LOGGING_CONFIG="-Dnop"
  fi
fi

if [ "$JPDA" = "jpda" ] ; then
  if [ -z "$JPDA_TRANSPORT" ]; then
    JPDA_TRANSPORT="dt_socket"
  fi
  if [ -z "$JPDA_ADDRESS" ]; then
    JPDA_ADDRESS="5005"
  fi
  if [ -z "$JPDA_SUSPEND" ]; then
    JPDA_SUSPEND="n"
  fi
  if [ -z "$JPDA_OPTS" ]; then
    JPDA_OPTS="-Djava.compiler=NONE -Xnoagent -Xdebug -Xrunjdwp:transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
  fi
  OPENIDM_OPTS="$OPENIDM_OPTS $JPDA_OPTS"
fi

CLASSPATH="$OPENIDM_HOME/bin/*:$OPENIDM_HOME/framework/*"

echo "Using OPENIDM_HOME:   $OPENIDM_HOME"
echo "Using PROJECT_HOME:   $PROJECT_HOME"
echo "Using OPENIDM_OPTS:   $OPENIDM_OPTS"
echo "Using LOGGING_CONFIG: $LOGGING_CONFIG"

# Keep track of this pid
echo $$ > "$OPENIDM_PID_FILE"

# Make the script location the current directory
cd "$PRGDIR"

# start in normal mode
exec java "$LOGGING_CONFIG" $JAVA_OPTS $OPENIDM_OPTS \
	-Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
	-classpath "$CLASSPATH" \
	-Dopenidm.system.server.root="$OPENIDM_HOME" \
	-Djava.awt.headless=true \
	org.forgerock.commons.launcher.Main -c "$OPENIDM_HOME"/bin/launcher.json $CLOPTS

# org.forgerock.commons.launcher.Main -c bin/launcher.json -w samples/sample1/cache -p samples/sample1 "$@"

cd -
