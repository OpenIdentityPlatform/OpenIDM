#!/bin/sh

# clean up left over pid files if necessary
cleanupPidFile() {
  if [ -f $OPENIDM_PID_FILE ]; then
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

echo $PRG

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Make the script location the current directory
cd $PRGDIR

# Only set OPENIDM_HOME if not already set
[ -z "$OPENIDM_HOME" ] && OPENIDM_HOME=`cd "$PRGDIR" >/dev/null; pwd`

# Only set OPENIDM_PID_FILE if not already set
[ -z "$OPENIDM_PID_FILE" ] && OPENIDM_PID_FILE=$OPENIDM_HOME/.openidm.pid

# Only set OPENIDM_OPTS if not already set
[ -z "$OPENIDM_OPTS" ] && OPENIDM_OPTS=${openidm.options}

# Set JDK Logger config file if it is present and an override has not been issued
if [ -z "$LOGGING_CONFIG" ]; then
  if [ -r "$OPENIDM_HOME"/conf/logging.properties ]; then
    LOGGING_CONFIG="-Djava.util.logging.config.file=$OPENIDM_HOME/conf/logging.properties"
  else
    LOGGING_CONFIG="-Dnop"
  fi
fi

if [ "$1" = "jpda" ] ; then
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
  shift
fi

CLASSPATH="$OPENIDM_HOME"/bin/felix.jar

echo "Using OPENIDM_HOME:   $OPENIDM_HOME"
echo "Using OPENIDM_OPTS:   $OPENIDM_OPTS"
echo "Using LOGGING_CONFIG: $LOGGING_CONFIG"

# Keep track of this pid
echo $$ > $OPENIDM_PID_FILE

# start in normal mode
exec java "$LOGGING_CONFIG" $JAVA_OPTS $OPENIDM_OPTS \
	-Djava.endorsed.dirs="$JAVA_ENDORSED_DIRS" \
    -Djava.security.auth.login.config=security/jaas-repo.conf \
	-classpath "$CLASSPATH" \
	-Dopenidm.system.server.root="$OPENIDM_HOME" \
	-Dignore.openidm.system.server.environment="dev|test|qa|prod" \
	org.apache.felix.main.Main "$@"
